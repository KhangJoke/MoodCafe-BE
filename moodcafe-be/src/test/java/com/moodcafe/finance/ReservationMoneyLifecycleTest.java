package com.moodcafe.finance;

import com.moodcafe.finance.service.ReservationRefundService;
import com.moodcafe.finance.service.ReservationSettlementService;
import com.moodcafe.loyalty.service.LoyaltyRewardService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.CustomerWalletService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationMoneyLifecycleTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MerchantWalletService merchantWalletService;

    @Mock
    private CustomerWalletService customerWalletService;

    @Mock
    private VoucherLifecycleService voucherLifecycleService;

    @Mock
    private LoyaltyRewardService loyaltyRewardService;

    @InjectMocks
    private ReservationRefundService refundService;

    @InjectMocks
    private ReservationSettlementService settlementService;

    private UUID reservationId;
    private UUID storeId;
    private UUID userId;
    private UUID voucherId;

    @BeforeEach
    void setUp() {
        reservationId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        voucherId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Cancel and Refund: Reverts merchant pending, credits 90k PayOS as MoodPoints, restores voucher")
    void testCancelAndRefundSuccess() {
        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .storeId(storeId)
                .userId(userId)
                .voucherId(voucherId)
                .status("PAID_CONFIRMED")
                .bookingTime(Instant.now().plus(5, ChronoUnit.HOURS)) // Well within 2h cancel window
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(new BigDecimal("10000"))
                .customerPaidAmount(new BigDecimal("90000"))
                .commissionAmount(new BigDecimal("15000"))
                .merchantNetAmount(new BigDecimal("85000"))
                .build();

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = refundService.cancelAndRefund(reservationId, userId, "Bận việc đột xuất");

        assertNotNull(result);
        assertEquals("CANCELLED_REFUNDED", result.getStatus());

        // 1. Revert merchant pending 85k
        verify(merchantWalletService).revertPending(eq(storeId), eq(new BigDecimal("85000")), any(), any());

        // 2. Refund 90k PayOS into MoodPoints 1:1
        verify(customerWalletService).creditRefundPoints(argThat(cmd ->
                cmd.getUserId().equals(userId) &&
                cmd.getPoints().equals(90000L) &&
                cmd.getBusinessKey().contains("REFUND_PAYOS_RES_")
        ));

        // 3. Restore applied voucher
        verify(voucherLifecycleService).restoreVoucherForRefund(eq(reservationId), eq(voucherId));
    }

    @Test
    @DisplayName("Settle COMPLETED: Moves 85k net from pending to available and awards SPEND points")
    void testSettleCompleted() {
        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .storeId(storeId)
                .userId(userId)
                .status("CHECKED_IN")
                .grossAmount(new BigDecimal("100000"))
                .merchantNetAmount(new BigDecimal("85000"))
                .build();

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Reservation settled = settlementService.settleReservation(reservationId, "COMPLETED");

        assertEquals("COMPLETED", settled.getStatus());
        verify(merchantWalletService).settleToAvailable(eq(storeId), eq(new BigDecimal("85000")), eq(reservationId.toString()), any());
        verify(loyaltyRewardService).awardSpendReward(reservation);
    }

    @Test
    @DisplayName("Settle EXPIRED_NO_SHOW: Guarantees 85k net to merchant, customer receives 0 points")
    void testSettleExpiredNoShow() {
        Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .storeId(storeId)
                .userId(userId)
                .status("TABLE_EXPIRED")
                .grossAmount(new BigDecimal("100000"))
                .merchantNetAmount(new BigDecimal("85000"))
                .build();

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Reservation settled = settlementService.settleReservation(reservationId, "EXPIRED_NO_SHOW");

        assertEquals("EXPIRED_NO_SHOW", settled.getStatus());
        verify(merchantWalletService).settleToAvailable(eq(storeId), eq(new BigDecimal("85000")), eq(reservationId.toString()), any());
        verify(loyaltyRewardService, never()).awardSpendReward(any());
    }
}
