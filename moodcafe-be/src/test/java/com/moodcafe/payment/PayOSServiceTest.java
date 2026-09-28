package com.moodcafe.payment;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.dto.request.PayOSWebhookRequest;
import com.moodcafe.payment.service.PayOSService;
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

    private PayOSService payOSService;

    @BeforeEach
    void setUp() {
        payOSService = new PayOSService(
                payOS,
                payOSConfig,
                subscriptionPaymentRepository,
                subscriptionService,
                userRepository,
                currentUserService
        );
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
}
