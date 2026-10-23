package com.moodcafe.reservation;

import com.moodcafe.payment.dto.GatewayPaymentObservation;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.scheduler.ReservationLifecycleScheduler;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentReconciliationIT {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationPaymentCaptureService captureService;
    @Mock
    private PayOSService payOSService;
    @Mock
    private PaymentAttemptService paymentAttemptService;
    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private VoucherLifecycleService voucherLifecycleService;
    @Mock
    private PayOS payOS;
    @Mock
    private PaymentRequestsService paymentRequestsService;
    @Mock
    private com.moodcafe.payment.config.PayOSConfig payOSConfig;

    private ReservationLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationLifecycleScheduler(
                reservationRepository,
                null,
                captureService,
                payOSService,
                paymentAttemptService,
                customerVoucherRepository,
                voucherLifecycleService
        );
    }

    @Test
    @DisplayName("checkPaymentStatus captures reservation payment when gateway returns PAID")
    void checkPaymentStatusCapturesReservation() {
        PayOSService realPayOSService = new com.moodcafe.payment.service.PayOSServiceImpl(
                payOS,
                payOSConfig,
                org.mockito.Mockito.mock(com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository.class),
                null,
                null,
                null,
                reservationRepository,
                captureService,
                paymentAttemptService
        );

        when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        when(payOSConfig.isConfigured()).thenReturn(true);

        Long orderCode = 2000000099L;
        PaymentLink link = org.mockito.Mockito.mock(PaymentLink.class);
        when(link.getAmountPaid()).thenReturn(100000L);
        when(link.getStatus()).thenReturn(PaymentLinkStatus.PAID);
        when(paymentRequestsService.get(orderCode)).thenReturn(link);

        UUID resId = UUID.randomUUID();
        Reservation res = Reservation.builder()
                .reservationId(resId)
                .payosOrderCode(orderCode)
                .status("PENDING_PAYMENT")
                .build();
        when(reservationRepository.findByPayosOrderCode(orderCode)).thenReturn(Optional.of(res));

        boolean result = realPayOSService.checkPaymentStatus(orderCode);

        assertThat(result).isTrue();
        verify(captureService).capturePaymentSuccess(resId, orderCode, 100000L);
        verify(paymentAttemptService).markStatus(orderCode, "PAID");
    }

    @Test
    @DisplayName("Sweep skips cancellation when gateway status is UNKNOWN (network error / timeout)")
    void sweepSkipsWhenGatewayStatusIsUnknown() {
        when(payOSService.isConfigured()).thenReturn(true);

        UUID resId = UUID.randomUUID();
        Long orderCode = 2000000100L;
        Reservation res = Reservation.builder()
                .reservationId(resId)
                .payosOrderCode(orderCode)
                .status("PENDING_PAYMENT")
                .createdAt(Instant.now().minus(20, ChronoUnit.MINUTES))
                .build();

        when(reservationRepository.findAll()).thenReturn(List.of(res));
        when(paymentAttemptService.reconcile(orderCode)).thenReturn(
                GatewayPaymentObservation.builder()
                        .orderCode(orderCode)
                        .status(GatewayPaymentObservation.GatewayStatus.UNKNOWN)
                        .build()
        );

        scheduler.sweepExpiredPaymentHolds();

        // Never expire or cancel when status is UNKNOWN
        verify(captureService, never()).handlePaymentExpiredOrFailed(any(), any());
    }

    @Test
    @DisplayName("Sweep captures orphan voucher if reservation was PAID_CONFIRMED instead of releasing")
    void sweepCapturesOrphanVoucherIfReservationPaid() {
        UUID resId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();

        CustomerVoucher orphan = CustomerVoucher.builder()
                .voucherId(voucherId)
                .status("LOCKED")
                .lockedReservationId(resId)
                .holdUntil(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        Reservation paidRes = Reservation.builder()
                .reservationId(resId)
                .status("PAID_CONFIRMED")
                .build();

        when(customerVoucherRepository.findAll()).thenReturn(List.of(orphan));
        when(reservationRepository.findById(resId)).thenReturn(Optional.of(paidRes));

        scheduler.sweepExpiredPaymentHolds();

        // Must capture voucher, NOT release back to available
        verify(voucherLifecycleService).captureVoucher(resId);
        verify(voucherLifecycleService, never()).releaseVoucher(any());
    }
}
