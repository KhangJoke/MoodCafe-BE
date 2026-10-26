package com.moodcafe.payment;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.service.PayOSServiceImpl;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOSServiceTest {

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
    private com.moodcafe.sponsored.abstraction.repository.SponsoredListingRepository sponsoredListingRepository;

    @Mock
    private com.moodcafe.sponsored.abstraction.service.SponsoredListingService sponsoredListingService;

    private PayOSServiceImpl payOSService;

    @BeforeEach
    void setUp() {
        payOSService = new PayOSServiceImpl(
                payOS,
                payOSConfig,
                subscriptionPaymentRepository,
                subscriptionService,
                userRepository,
                currentUserService
        );
        payOSService.setSponsoredListingRepository(sponsoredListingRepository);
        payOSService.setSponsoredListingService(sponsoredListingService);
    }

    @Test
    @DisplayName("sanitizeDescription should strip diacritics, special chars and limit to 25 chars")
    void testSanitizeDescription() {
        String input1 = "Đơn hàng cà phê số 123456789";
        String output1 = payOSService.sanitizeDescription(input1);
        assertTrue(output1.length() <= 25);
        assertFalse(output1.contains("Đ") || output1.contains("đ") || output1.contains("à") || output1.contains("ê"));

        String inputSpecial = "MOOD_PRO#123@!$%&*";
        String outputSpecial = payOSService.sanitizeDescription(inputSpecial);
        assertEquals("MOODPRO123", outputSpecial);

        String nullInput = payOSService.sanitizeDescription(null);
        assertEquals("MOODCAFE", nullInput);
    }

    @Test
    @DisplayName("handlePayOSWebhook should confirm payment when orderCode matches pending payment")
    void testHandlePayOSWebhook_Success() {
        long orderCode = 1712345678L;
        String txCode = String.valueOf(orderCode);

        PayOSWebhookRequest.PayOSWebhookData data = new PayOSWebhookRequest.PayOSWebhookData();
        data.setOrderCode(orderCode);
        data.setAmount(100000.0);
        data.setDescription("MOOD PRO");

        PayOSWebhookRequest request = new PayOSWebhookRequest();
        request.setCode("00");
        request.setData(data);

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .paymentId(UUID.randomUUID())
                .transactionCode(txCode)
                .amount(BigDecimal.valueOf(100000))
                .status(SubscriptionPaymentStatus.PENDING)
                .build();

        when(payOSConfig.isConfigured()).thenReturn(true);
        vn.payos.service.blocking.webhooks.WebhooksService webhooksMock = mock(vn.payos.service.blocking.webhooks.WebhooksService.class);
        when(payOS.webhooks()).thenReturn(webhooksMock);
        when(subscriptionPaymentRepository.findByTransactionCode(txCode)).thenReturn(Optional.of(payment));

        payOSService.handlePayOSWebhook(request);

        verify(subscriptionService, times(1)).confirmPayment(txCode);
    }

    @Test
    @DisplayName("handlePayOSWebhook should handle null request gracefully without throwing exception")
    void testHandlePayOSWebhook_NullRequest() {
        assertDoesNotThrow(() -> payOSService.handlePayOSWebhook(null));
        assertDoesNotThrow(() -> payOSService.handlePayOSWebhook(new PayOSWebhookRequest()));
    }

    @Test
    @DisplayName("handlePayOSWebhook should confirm sponsored campaign when orderCode matches pending campaign")
    void testHandlePayOSWebhook_SponsoredCampaign_Success() {
        long orderCode = 1719999999L;
        String txCode = String.valueOf(orderCode);

        PayOSWebhookRequest.PayOSWebhookData data = new PayOSWebhookRequest.PayOSWebhookData();
        data.setOrderCode(orderCode);
        data.setAmount(200000.0);
        data.setDescription("MOOD SPONSOR");

        PayOSWebhookRequest request = new PayOSWebhookRequest();
        request.setCode("00");
        request.setData(data);

        com.moodcafe.sponsored.entity.SponsoredListing campaign = com.moodcafe.sponsored.entity.SponsoredListing.builder()
                .sponsoredListingId(UUID.randomUUID())
                .transactionCode(txCode)
                .amount(BigDecimal.valueOf(200000))
                .status(com.moodcafe.sponsored.entity.enums.SponsoredListingStatus.PENDING)
                .build();

        when(payOSConfig.isConfigured()).thenReturn(true);
        when(payOS.webhooks()).thenReturn(mock(vn.payos.service.blocking.webhooks.WebhooksService.class));
        when(subscriptionPaymentRepository.findByTransactionCode(txCode)).thenReturn(Optional.empty());
        when(sponsoredListingRepository.findByTransactionCode(txCode)).thenReturn(Optional.of(campaign));

        payOSService.handlePayOSWebhook(request);

        verify(sponsoredListingService, times(1)).confirmCampaignPayment(txCode);
    }

    @Test
    void verifiedWebhookStillConfirmsLegacyBooking() {
        var bookingRepository = mock(com.moodcafe.booking.abstraction.repository.BookingRepository.class);
        var bookingService = mock(com.moodcafe.booking.abstraction.service.BookingService.class);
        payOSService.setBookingRepository(bookingRepository);
        payOSService.setBookingService(bookingService);
        long orderCode = 1800000001L;
        var booking = com.moodcafe.booking.entity.Booking.builder()
                .status(com.moodcafe.booking.entity.enums.BookingStatus.PENDING_PAYMENT).build();
        when(bookingRepository.findByPaymentOrderCode(orderCode)).thenReturn(Optional.of(booking));
        when(payOSConfig.isConfigured()).thenReturn(true);
        when(payOS.webhooks()).thenReturn(mock(vn.payos.service.blocking.webhooks.WebhooksService.class));

        payOSService.handlePayOSWebhook(successWebhook(orderCode));

        verify(bookingService).confirmBookingPayment(orderCode, 100000L);
    }

    @Test
    void cancelledBookingPaymentSessionDoesNotCancelTheBooking() {
        long orderCode = 1800000004L;
        var bookingRepository = mock(com.moodcafe.booking.abstraction.repository.BookingRepository.class);
        var bookingService = mock(com.moodcafe.booking.abstraction.service.BookingService.class);
        payOSService.setBookingRepository(bookingRepository);
        payOSService.setBookingService(bookingService);
        when(payOSConfig.isConfigured()).thenReturn(true);

        var requests = mock(vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        var link = mock(vn.payos.model.v2.paymentRequests.PaymentLink.class);
        when(payOS.paymentRequests()).thenReturn(requests);
        when(requests.get(orderCode)).thenReturn(link);
        when(link.getStatus()).thenReturn(vn.payos.model.v2.paymentRequests.PaymentLinkStatus.CANCELLED);

        assertFalse(payOSService.checkPaymentStatus(orderCode));

        verify(bookingService, never()).timeoutPendingBooking(any());
    }

    @Test
    void duplicateCreateAfterAcceptedTimeoutRecoversSameGatewayLink() {
        when(payOSConfig.getReturnUrl()).thenReturn("https://app.test/payment/return");
        when(payOSConfig.getCancelUrl()).thenReturn("https://app.test/payment/return");
        when(payOSConfig.isConfigured()).thenReturn(true);
        var requests = mock(vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(requests);
        when(requests.create(any(vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest.class)))
                .thenThrow(new RuntimeException("accepted but response lost / duplicate order"));
        var link = mock(vn.payos.model.v2.paymentRequests.PaymentLink.class);
        when(link.getId()).thenReturn("124c33293c934a85be5b7f8761a27a07");
        lenient().when(link.getOrderCode()).thenReturn(123L);
        lenient().when(link.getAmount()).thenReturn(100000L);
        lenient().when(link.getAmountPaid()).thenReturn(0L);
        lenient().when(link.getStatus()).thenReturn(vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PENDING);
        when(requests.get(123L)).thenReturn(link);
        var response = payOSService.createBookingPaymentLink(UUID.randomUUID(), 123L, 100000L, "DATBAN 123", null);
        assertEquals("https://pay.payos.vn/web/" + link.getId(), response.getCheckoutUrl());
        assertEquals(link.getId(), response.getPaymentLinkId());
        assertEquals(123L, response.getOrderCode());
    }

    private PayOSWebhookRequest successWebhook(long orderCode) {
        var data = new PayOSWebhookRequest.PayOSWebhookData();
        data.setOrderCode(orderCode);
        data.setAmount(100000.0);
        data.setCurrency("VND");
        var request = new PayOSWebhookRequest();
        request.setCode("00");
        request.setData(data);
        return request;
    }
}
