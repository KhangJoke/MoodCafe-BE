package com.moodcafe.payment;

import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationPaymentIntegrationTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MerchantWalletService merchantWalletService;

    @Mock
    private VoucherLifecycleService voucherLifecycleService;

    @InjectMocks
    private ReservationPaymentCaptureService captureService;

    private UUID storeId;
    private UUID reservationId;
    private UUID voucherId;
    private long orderCode;
    private Reservation pendingReservation;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        voucherId = UUID.randomUUID();
        orderCode = 123456789L;

        pendingReservation = Reservation.builder()
                .reservationId(reservationId)
                .storeId(storeId)
                .userId(UUID.randomUUID())
                .voucherId(voucherId)
                .grossAmount(new BigDecimal("100000.00"))
                .voucherDiscount(new BigDecimal("90000.00"))
                .customerPaidAmount(new BigDecimal("10000.00")) // PayOS payable amount is 10k!
                .commissionAmount(new BigDecimal("15000.00"))
                .merchantNetAmount(new BigDecimal("85000.00"))
                .payosOrderCode(orderCode)
                .status("PENDING_PAYMENT")
                .build();
    }

    @Test
    @DisplayName("1. Successful PayOS capture: Transitions to PAID_CONFIRMED, holds net in pending, captures voucher")
    void capturePaymentSuccess() {
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        captureService.capturePaymentSuccess(reservationId, orderCode, 10000L);

        assertEquals("PAID_CONFIRMED", pendingReservation.getStatus());
        assertEquals(0, pendingReservation.getActualPaidAmount().compareTo(new BigDecimal("10000.00")));
        assertNotNull(pendingReservation.getPaidAt());

        // Verify merchant net amount held in pending
        verify(merchantWalletService).holdPending(
                eq(storeId),
                eq(new BigDecimal("85000.00")),
                eq(reservationId.toString()),
                any()
        );

        // Verify voucher captured permanently
        verify(voucherLifecycleService).captureVoucher(eq(reservationId));
    }

    @Test
    @DisplayName("2. Idempotent webhook: Duplicate callback does not double credit merchant pending")
    void duplicateCaptureIsIdempotent() {
        pendingReservation.setStatus("PAID_CONFIRMED");
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(pendingReservation));

        captureService.capturePaymentSuccess(reservationId, orderCode, 10000L);

        verify(merchantWalletService, never()).holdPending(any(), any(), any(), any());
        verify(voucherLifecycleService, never()).captureVoucher(any());
    }

    @Test
    @DisplayName("3. Underpayment protection: Paying less than required amount is rejected")
    void underpaymentRejected() {
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(pendingReservation));

        // Required 10k, but only paid 5k
        AppException ex = assertThrows(AppException.class, () ->
                captureService.capturePaymentSuccess(reservationId, orderCode, 5000L));

        assertTrue(ex.getMessage().contains("không khớp với đơn đặt bàn"));
        assertEquals("PENDING_PAYMENT", pendingReservation.getStatus());
        verify(merchantWalletService, never()).holdPending(any(), any(), any(), any());
    }
}
