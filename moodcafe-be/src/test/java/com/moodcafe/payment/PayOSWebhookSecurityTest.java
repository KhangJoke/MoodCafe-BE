package com.moodcafe.payment;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.abstraction.service.PayOSService;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSWebhookSecurityTest {

    @Mock
    private PayOS payOS;

    @Mock
    private PayOSConfig payOSConfig;

    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationPaymentCaptureService reservationPaymentCaptureService;

    private PayOSService payOSService;

    @BeforeEach
    void setUp() {
        payOSService = new com.moodcafe.payment.service.PayOSServiceImpl(
                payOS,
                payOSConfig,
                subscriptionPaymentRepository,
                subscriptionService,
                userRepository,
                currentUserService,
                reservationRepository,
                reservationPaymentCaptureService,
                null
        );
    }

    private PayOSWebhookRequest createSampleRequest(Long orderCode, Double amount, String code, String currency) {
        PayOSWebhookRequest.PayOSWebhookData data = new PayOSWebhookRequest.PayOSWebhookData();
        data.setOrderCode(orderCode);
        data.setAmount(amount);
        data.setCurrency(currency);
        data.setDescription("RES BOOKING 123");

        PayOSWebhookRequest req = new PayOSWebhookRequest();
        req.setCode(code);
        req.setData(data);
        req.setSignature("test-signature");
        return req;
    }

    @Test
    @DisplayName("R1: Webhook fails closed when signature verification fails")
    void testWebhook_InvalidSignature_FailsClosed() throws Exception {
        when(payOSConfig.isConfigured()).thenReturn(true);
        var webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);
        doThrow(new RuntimeException("Signature verification failed")).when(webhooksMock).verify(any());

        PayOSWebhookRequest request = createSampleRequest(999999L, 100000.0, "00", "VND");

        AppException ex = assertThrows(AppException.class, () -> payOSService.handlePayOSWebhook(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chữ ký webhook PayOS không hợp lệ"));

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
        verify(subscriptionService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("R1: Webhook fails closed when gateway is unconfigured")
    void testWebhook_GatewayUnconfigured_FailsClosed() {
        when(payOSConfig.isConfigured()).thenReturn(false);

        PayOSWebhookRequest request = createSampleRequest(999999L, 100000.0, "00", "VND");

        AppException ex = assertThrows(AppException.class, () -> payOSService.handlePayOSWebhook(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Cổng thanh toán PayOS chưa được kích hoạt"));

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
    }

    @Test
    @DisplayName("R1: Webhook does not capture when response code indicates failure")
    void testWebhook_NonSuccessCode_DoesNotCapture() throws Exception {
        when(payOSConfig.isConfigured()).thenReturn(true);
        var webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);

        PayOSWebhookRequest request = createSampleRequest(999999L, 100000.0, "01", "VND"); // "01" is failure

        payOSService.handlePayOSWebhook(request);

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
        verify(subscriptionService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("R1: Webhook rejects null or non-positive amount")
    void testWebhook_MissingAmount_Rejected() throws Exception {
        when(payOSConfig.isConfigured()).thenReturn(true);
        var webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);

        PayOSWebhookRequest request = createSampleRequest(999999L, null, "00", "VND");

        AppException ex = assertThrows(AppException.class, () -> payOSService.handlePayOSWebhook(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Số tiền thanh toán trong webhook không hợp lệ"));

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
    }

    @Test
    @DisplayName("R1: Webhook rejects non-VND currency")
    void testWebhook_InvalidCurrency_Rejected() throws Exception {
        when(payOSConfig.isConfigured()).thenReturn(true);
        var webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);

        PayOSWebhookRequest request = createSampleRequest(999999L, 100000.0, "00", "USD");

        AppException ex = assertThrows(AppException.class, () -> payOSService.handlePayOSWebhook(request));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Đơn vị tiền tệ không hợp lệ"));

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
    }

    @Test
    @DisplayName("R1: Webhook ping test without data is accepted without capture side effects")
    void testWebhook_PingTest_AcknowledgedGracefully() {
        PayOSWebhookRequest request = new PayOSWebhookRequest();
        request.setData(null);

        assertDoesNotThrow(() -> payOSService.handlePayOSWebhook(request));

        verify(reservationPaymentCaptureService, never()).capturePaymentSuccess(any(), any(), any());
        verify(subscriptionService, never()).confirmPayment(any());
    }

    @Test
    @DisplayName("R1: Valid webhook successfully captures reservation")
    void testWebhook_ValidReservationPayment_CapturedSuccessfully() throws Exception {
        when(payOSConfig.isConfigured()).thenReturn(true);
        var webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);

        long orderCode = 123456789L;
        UUID resId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .reservationId(resId)
                .payosOrderCode(orderCode)
                .customerPaidAmount(BigDecimal.valueOf(100000))
                .status("PENDING_PAYMENT")
                .build();

        when(subscriptionPaymentRepository.findByTransactionCode(String.valueOf(orderCode))).thenReturn(Optional.empty());
        when(reservationRepository.findByPayosOrderCode(orderCode)).thenReturn(Optional.of(reservation));

        PayOSWebhookRequest request = createSampleRequest(orderCode, 100000.0, "00", "VND");

        payOSService.handlePayOSWebhook(request);

        verify(reservationPaymentCaptureService, times(1)).capturePaymentSuccess(resId, orderCode, 100000L);
    }
}
