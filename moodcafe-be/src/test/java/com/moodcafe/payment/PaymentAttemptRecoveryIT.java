package com.moodcafe.payment;

import com.moodcafe.finance.dto.ReservationFinancialSnapshot;
import com.moodcafe.finance.dto.ReservationPricingInput;
import com.moodcafe.finance.service.ReservationPricingService;
import com.moodcafe.payment.dto.response.PayOSResponse;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.service.BookingPaymentCoordinator;
import com.moodcafe.payment.service.OrderCodeGeneratorService;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.abstraction.repository.TableCategoryRepository;
import com.moodcafe.reservation.dto.CreateReservationRequest;
import com.moodcafe.reservation.dto.ReservationResponse;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.entity.TableCategory;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.reservation.service.ReservationService;
import com.moodcafe.reservation.service.ServiceDayDeadlineResolver;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.VoucherPointAllocationRepository;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.MerchantWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAttemptRecoveryIT {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TableCategoryRepository tableCategoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ServiceDayDeadlineResolver serviceDayDeadlineResolver;
    @Mock
    private OrderCodeGeneratorService orderCodeGeneratorService;
    @Mock
    private PaymentAttemptService paymentAttemptService;
    @Mock
    private ReservationPricingService pricingService;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherPointAllocationRepository allocationRepository;
    @Mock
    private VoucherLifecycleService voucherLifecycleService;
    @Mock
    private MerchantWalletService merchantWalletService;
    @Mock
    private PayOSService payOSService;
    @Mock
    private ReservationPaymentCaptureService captureService;

    private ReservationService reservationService;
    private BookingPaymentCoordinator coordinator;

    private UUID storeId;
    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        reservationService = new ReservationService(
                reservationRepository,
                tableCategoryRepository,
                storeRepository,
                serviceDayDeadlineResolver,
                orderCodeGeneratorService,
                paymentAttemptService,
                pricingService,
                customerVoucherRepository,
                allocationRepository,
                voucherLifecycleService,
                merchantWalletService,
                null,
                null,
                null  // qrVerificationService
        );

        coordinator = new BookingPaymentCoordinator(
                reservationService,
                reservationRepository,
                payOSService,
                paymentAttemptService,
                captureService
        );
    }

    @Test
    @DisplayName("Durable payment attempt created and survives PayOS gateway timeout without rollback")
    void gatewayTimeoutPreservesAttemptAndReservation() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .bookingTime(Instant.now().plusSeconds(3600))
                .guestCount(2)
                .build();

        TableCategory category = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .minSpendAmount(new BigDecimal("100000"))
                .isActive(true)
                .isDeleted(false)
                .build();
        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ReservationFinancialSnapshot snapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(new BigDecimal("100000"))
                .voucherDiscount(BigDecimal.ZERO)
                .customerPaidAmount(new BigDecimal("100000"))
                .commissionAmount(new BigDecimal("10000"))
                .merchantNetAmount(new BigDecimal("90000"))
                .build();
        when(pricingService.quote(any(ReservationPricingInput.class))).thenReturn(snapshot);
        when(serviceDayDeadlineResolver.resolve(any(), any(), any(), any())).thenReturn(Instant.now().plusSeconds(7200));
        when(orderCodeGeneratorService.nextOrderCode()).thenReturn(2000000001L);

        UUID resId = UUID.randomUUID();
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(resId);
            return r;
        });

        when(payOSService.isConfigured()).thenReturn(true);
        // Simulate remote network timeout or gateway failure
        when(payOSService.createBookingPaymentLink(eq(resId), eq(2000000001L), eq(100000L), anyString()))
                .thenThrow(new RuntimeException("Gateway connection timed out after 10000ms"));

        assertThatThrownBy(() -> coordinator.createReservation(request, userId, "INTENT_TIMEOUT_1"))
                .isInstanceOf(AppException.class)
                .matches(e -> ((AppException) e).getErrorCode() == ErrorCode.INTERNAL_ERROR);

        // Verify PaymentAttempt was created BEFORE gateway call
        verify(paymentAttemptService).createAttempt(
                eq("BOOKING"), eq(resId.toString()), eq(2000000001L),
                eq(new BigDecimal("100000")), eq("VND"), eq("INTENT_TIMEOUT_1"),
                eq(userId), anyString()
        );

        // Verify attempt was flagged UNKNOWN for reconciliation
        verify(paymentAttemptService).markStatus(2000000001L, "UNKNOWN");

        // Verify reservation was NOT deleted or cancelled
        verify(reservationRepository, never()).delete(any());
        verify(voucherLifecycleService, never()).releaseVoucher(any());
    }

    @Test
    @DisplayName("Idempotent retry returns existing reservation without allocating new orderCode")
    void idempotentRetryReturnsExistingReservation() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .bookingTime(Instant.now().plusSeconds(3600))
                .build();

        UUID existingResId = UUID.randomUUID();
        PaymentAttempt existingAttempt = PaymentAttempt.builder()
                .orderCode(2000000005L)
                .businessId(existingResId.toString())
                .status("PENDING")
                .intentKey("INTENT_DUP_1")
                .actorId(userId)
                .payloadHash(ReservationService.computeBookingPayloadHash(request))
                .build();

        when(paymentAttemptService.findByActorIdAndPurposeAndIntentKey(userId, "BOOKING", "INTENT_DUP_1"))
                .thenReturn(Optional.of(existingAttempt));

        Reservation existingReservation = Reservation.builder()
                .reservationId(existingResId)
                .storeId(storeId)
                .userId(userId)
                .status("PENDING_PAYMENT")
                .payosOrderCode(2000000005L)
                .customerPaidAmount(new BigDecimal("50000"))
                .paymentUrl("https://checkout.payos.vn/payment/link-5")
                .build();
        when(reservationRepository.findById(existingResId)).thenReturn(Optional.of(existingReservation));

        ReservationResponse response = coordinator.createReservation(request, userId, "INTENT_DUP_1");

        assertThat(response.getReservationId()).isEqualTo(existingResId);
        assertThat(response.getPayosOrderCode()).isEqualTo(2000000005L);
        assertThat(response.getPaymentUrl()).isEqualTo("https://checkout.payos.vn/payment/link-5");

        // Never generate new orderCode or call gateway again
        verify(orderCodeGeneratorService, never()).nextOrderCode();
        verify(payOSService, never()).createBookingPaymentLink(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Zero payable reservation confirms internally with status PAID and no gateway call")
    void zeroPayableConfirmsInternally() {
        UUID voucherId = UUID.randomUUID();
        CreateReservationRequest request = CreateReservationRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .bookingTime(Instant.now().plusSeconds(3600))
                .voucherId(voucherId)
                .build();

        com.moodcafe.voucher.entity.CustomerVoucher voucher = com.moodcafe.voucher.entity.CustomerVoucher.builder()
                .voucherId(voucherId)
                .userId(userId)
                .status("AVAILABLE")
                .faceValueVnd(new BigDecimal("50000"))
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
        when(customerVoucherRepository.findByIdWithLock(voucherId)).thenReturn(Optional.of(voucher));

        TableCategory category = TableCategory.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .minSpendAmount(new BigDecimal("50000"))
                .isActive(true)
                .isDeleted(false)
                .build();
        when(tableCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ReservationFinancialSnapshot snapshot = ReservationFinancialSnapshot.builder()
                .grossAmount(new BigDecimal("50000"))
                .voucherDiscount(new BigDecimal("50000"))
                .customerPaidAmount(BigDecimal.ZERO)
                .commissionAmount(new BigDecimal("5000"))
                .merchantNetAmount(new BigDecimal("45000"))
                .build();
        when(pricingService.quote(any(ReservationPricingInput.class))).thenReturn(snapshot);
        when(serviceDayDeadlineResolver.resolve(any(), any(), any(), any())).thenReturn(Instant.now().plusSeconds(7200));
        when(orderCodeGeneratorService.nextOrderCode()).thenReturn(2000000010L);

        UUID resId = UUID.randomUUID();
        Reservation prepared = Reservation.builder()
                .reservationId(resId)
                .storeId(storeId)
                .userId(userId)
                .status("PENDING_PAYMENT")
                .payosOrderCode(2000000010L)
                .customerPaidAmount(BigDecimal.ZERO)
                .merchantNetAmount(new BigDecimal("45000"))
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(prepared);
        when(reservationRepository.findById(resId)).thenReturn(Optional.of(prepared));

        ReservationResponse response = coordinator.createReservation(request, userId, "INTENT_ZERO_1");

        assertThat(response.getReservationId()).isEqualTo(resId);
        // Verify capture was executed directly
        verify(captureService).capturePaymentSuccess(resId, 2000000010L, 0L);
        verify(paymentAttemptService).markStatus(2000000010L, "PAID");
        // Verify gateway was never invoked
        verify(payOSService, never()).createBookingPaymentLink(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Idempotency replay with mismatched payload throws CONFLICT 409")
    void idempotencyReplayWithMismatchedPayloadThrowsConflict() {
        CreateReservationRequest originalRequest = CreateReservationRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .bookingTime(Instant.now().plusSeconds(3600))
                .build();

        UUID existingResId = UUID.randomUUID();
        PaymentAttempt existingAttempt = PaymentAttempt.builder()
                .orderCode(2000000005L)
                .businessId(existingResId.toString())
                .status("PENDING")
                .intentKey("INTENT_CONFLICT_1")
                .actorId(userId)
                .payloadHash(ReservationService.computeBookingPayloadHash(originalRequest))
                .build();

        when(paymentAttemptService.findByActorIdAndPurposeAndIntentKey(userId, "BOOKING", "INTENT_CONFLICT_1"))
                .thenReturn(Optional.of(existingAttempt));

        CreateReservationRequest modifiedRequest = CreateReservationRequest.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .bookingTime(Instant.now().plusSeconds(7200)) // different time!
                .build();

        assertThatThrownBy(() -> coordinator.createReservation(modifiedRequest, userId, "INTENT_CONFLICT_1"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Idempotency-Key đã được sử dụng với thông tin đặt bàn khác");
    }
}
