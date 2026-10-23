package com.moodcafe.payment;

import com.moodcafe.payment.abstraction.repository.PaymentAttemptRepository;
import com.moodcafe.payment.config.PayOSConfig;
import com.moodcafe.payment.entity.PaymentAttempt;
import com.moodcafe.payment.service.PaymentAttemptService;
import com.moodcafe.payment.dto.GatewayPaymentObservation.GatewayStatus;
import org.junit.jupiter.api.Test;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAttemptSafetyTest {
    @Test
    void delayedGatewayTimeoutCannotDowngradePaidAttempt() {
        var repo = mock(PaymentAttemptRepository.class);
        var attempt = PaymentAttempt.builder().status("PAID").orderCode(1L).build();
        when(repo.findByOrderCode(1L)).thenReturn(Optional.of(attempt));
        var service = new PaymentAttemptService(repo, mock(PayOS.class), mock(PayOSConfig.class));
        service.markStatus(1L, "UNKNOWN");
        assertEquals("PAID", attempt.getStatus());
        verify(repo, never()).save(any());
    }

    @Test
    void cancellationReconcilesAgainAndObservesPaymentThatWonRace() {
        var repo = mock(PaymentAttemptRepository.class);
        var payos = mock(PayOS.class);
        var config = mock(PayOSConfig.class);
        var requests = mock(PaymentRequestsService.class);
        when(config.isConfigured()).thenReturn(true);
        when(payos.paymentRequests()).thenReturn(requests);
        var pending = mock(PaymentLink.class);
        var paid = mock(PaymentLink.class);
        when(pending.getStatus()).thenReturn(PaymentLinkStatus.PENDING);
        when(pending.getAmountPaid()).thenReturn(0L);
        when(paid.getStatus()).thenReturn(PaymentLinkStatus.PAID);
        when(paid.getAmountPaid()).thenReturn(120L);
        when(requests.get(1L)).thenReturn(pending, paid);
        var observation = new PaymentAttemptService(repo, payos, config).cancelAndReconcile(1L);
        assertEquals(GatewayStatus.PAID, observation.getStatus());
        assertEquals(120L, observation.getAmount().longValueExact());
        verify(requests).cancel(eq(1L), anyString());
    }

    @Test
    void unavailableGatewayIsUnknownAndCannotBeCancelled() {
        var payos = mock(PayOS.class);
        var observation = new PaymentAttemptService(mock(PaymentAttemptRepository.class), payos,
                mock(PayOSConfig.class)).cancelAndReconcile(1L);
        assertEquals(GatewayStatus.UNKNOWN, observation.getStatus());
        verifyNoInteractions(payos);
    }
}
