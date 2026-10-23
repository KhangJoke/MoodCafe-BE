package com.moodcafe.payment;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.service.PayOSServiceImpl;
import com.moodcafe.reservation.abstraction.repository.ReservationRepository;
import com.moodcafe.reservation.entity.Reservation;
import com.moodcafe.reservation.service.ReservationPaymentCaptureService;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import org.junit.jupiter.api.Test;
import vn.payos.PayOS;
import vn.payos.crypto.CryptoProviderImpl;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayOSRealSdkWebhookTest {
    private static final String KEY = "offline-test-checksum-key";
    private static final long ORDER_CODE = 1800000099L;
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ReservationPaymentCaptureService capture = mock(ReservationPaymentCaptureService.class);

    private PayOSServiceImpl service() {
        PayOSConfig config = mock(PayOSConfig.class);
        when(config.isConfigured()).thenReturn(true);
        return new PayOSServiceImpl(new PayOS("offline-client", "offline-api", KEY), config,
                mock(SubscriptionPaymentRepository.class), null, null, null, reservations, capture, null);
    }

    private PayOSWebhookRequest signedRequest() throws Exception {
        WebhookData data = WebhookData.builder().orderCode(ORDER_CODE).amount(100000L)
                .description("DATBAN 1800000099").accountNumber("123456789")
                .reference("reference-123").transactionDateTime("2026-10-23 10:00:00")
                .currency("VND").paymentLinkId("link-123").code("00").desc("success")
                .counterAccountBankId("970422").counterAccountBankName("MB")
                .counterAccountName("CUSTOMER NAME").counterAccountNumber("0987654321")
                .virtualAccountName("MOODCAFE").virtualAccountNumber("VA123456")
                .build();
        String signature = new CryptoProviderImpl().createSignatureFromObj(data, KEY);
        Webhook webhook = Webhook.builder().code("00").desc("success").success(true)
                .data(data).signature(signature).build();
        return mapper.readValue(mapper.writeValueAsString(webhook), PayOSWebhookRequest.class);
    }

    @Test
    void realSdkAcceptsSignedIncomingPayloadWithAllCounterAndVirtualAccountFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(reservations.findByPayosOrderCode(ORDER_CODE)).thenReturn(Optional.of(
                Reservation.builder().reservationId(id).status("PENDING_PAYMENT").build()));
        var request = signedRequest();

        assertDoesNotThrow(() -> service().handlePayOSWebhook(request));

        verify(capture).capturePaymentSuccess(id, ORDER_CODE, 100000L);
    }

    @Test
    void realSdkAcceptsPayloadDeserializedBySpringJackson3() throws Exception {
        var jackson3 = tools.jackson.databind.json.JsonMapper.builder().build();
        var request = jackson3.readValue(mapper.writeValueAsString(signedRequest()), PayOSWebhookRequest.class);
        UUID id = UUID.randomUUID();
        when(reservations.findByPayosOrderCode(ORDER_CODE)).thenReturn(Optional.of(
                Reservation.builder().reservationId(id).status("PENDING_PAYMENT").build()));

        assertDoesNotThrow(() -> service().handlePayOSWebhook(request));

        verify(capture).capturePaymentSuccess(id, ORDER_CODE, 100000L);
    }

    @Test
    void realSdkRejectsTamperedSignedAccountMetadataWithoutCapture() throws Exception {
        var request = signedRequest();
        request.getData().putAdditionalField("counterAccountName", "TAMPERED CUSTOMER");

        AppException error = assertThrows(AppException.class, () -> service().handlePayOSWebhook(request));
        assertTrue(error.getMessage().contains("Data not integrity"), error.getMessage());

        verifyNoInteractions(capture, reservations);
    }

    @Test
    void realSdkRejectsTamperedAmountWithoutCapture() throws Exception {
        var request = signedRequest();
        request.getData().setAmount(200000.0);

        AppException error = assertThrows(AppException.class, () -> service().handlePayOSWebhook(request));
        assertTrue(error.getMessage().contains("Data not integrity"), error.getMessage());

        verifyNoInteractions(capture, reservations);
    }
}
