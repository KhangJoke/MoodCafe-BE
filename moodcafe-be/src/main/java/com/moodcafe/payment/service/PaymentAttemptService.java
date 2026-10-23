package com.moodcafe.payment.service;

import com.moodcafe.payment.abstraction.repository.PaymentAttemptRepository;
import com.moodcafe.payment.dto.GatewayPaymentObservation;
import com.moodcafe.payment.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAttemptService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PayOS payOS;
    private final com.moodcafe.payment.config.PayOSConfig payOSConfig;

    @Transactional(propagation = Propagation.REQUIRED)
    public PaymentAttempt createAttempt(
            String purpose,
            String businessId,
            Long orderCode,
            BigDecimal amount,
            String currency,
            String intentKey
    ) {
        return createAttempt(purpose, businessId, orderCode, amount, currency, intentKey, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public PaymentAttempt createAttempt(
            String purpose,
            String businessId,
            Long orderCode,
            BigDecimal amount,
            String currency,
            String intentKey,
            UUID actorId,
            String payloadHash
    ) {
        PaymentAttempt attempt = PaymentAttempt.builder()
                .purpose(purpose)
                .businessId(businessId)
                .orderCode(orderCode)
                .amount(amount)
                .currency(currency != null ? currency : "VND")
                .status("PENDING")
                .intentKey(intentKey)
                .actorId(actorId)
                .payloadHash(payloadHash)
                .build();
        return paymentAttemptRepository.save(attempt);
    }

    public Optional<PaymentAttempt> findByOrderCode(Long orderCode) {
        return paymentAttemptRepository.findByOrderCode(orderCode);
    }

    public Optional<PaymentAttempt> findByIntentKey(String intentKey) {
        if (intentKey == null || intentKey.isBlank()) {
            return Optional.empty();
        }
        return paymentAttemptRepository.findByIntentKey(intentKey);
    }

    public Optional<PaymentAttempt> findByActorIdAndPurposeAndIntentKey(UUID actorId, String purpose, String intentKey) {
        if (actorId == null || purpose == null || intentKey == null || intentKey.isBlank()) {
            return Optional.empty();
        }
        return paymentAttemptRepository.findByActorIdAndPurposeAndIntentKey(actorId, purpose, intentKey.trim());
    }

    @Transactional
    public void recordGatewayResponse(Long orderCode, String paymentLinkId, String checkoutUrl, String qrCodeUrl) {
        paymentAttemptRepository.findByOrderCode(orderCode).ifPresent(attempt -> {
            attempt.setGatewayPaymentLinkId(paymentLinkId);
            attempt.setCheckoutUrl(checkoutUrl);
            attempt.setQrCodeUrl(qrCodeUrl);
            paymentAttemptRepository.save(attempt);
        });
    }

    @Transactional
    public void markStatus(Long orderCode, String status) {
        paymentAttemptRepository.findByOrderCode(orderCode).ifPresent(attempt -> {
            if ("PAID".equals(attempt.getStatus()) && !"PAID".equals(status)) return;
            attempt.setStatus(status);
            paymentAttemptRepository.save(attempt);
        });
    }

    public GatewayPaymentObservation cancelAndReconcile(Long orderCode) {
        GatewayPaymentObservation before = reconcile(orderCode);
        if (before.getStatus() != GatewayPaymentObservation.GatewayStatus.PENDING) return before;
        try {
            payOS.paymentRequests().cancel(orderCode, "Booking payment window closed");
        } catch (Exception e) {
            log.warn("Gateway cancellation outcome unknown for {}", orderCode);
        }
        // A cancelled response can race with capture. Read the final gateway state again.
        return reconcile(orderCode);
    }

    /**
     * Reconciles payment status against the gateway.
     * Returns UNKNOWN on network timeout or configuration absence, NEVER falsely assuming failure.
     */
    public GatewayPaymentObservation reconcile(Long orderCode) {
        if (orderCode == null || !payOSConfig.isConfigured() || payOS == null) {
            log.warn("Cannot reconcile orderCode {}: gateway unconfigured", orderCode);
            return GatewayPaymentObservation.builder()
                    .orderCode(orderCode)
                    .status(GatewayPaymentObservation.GatewayStatus.UNKNOWN)
                    .build();
        }

        try {
            PaymentLink paymentLink = payOS.paymentRequests().get(orderCode);
            if (paymentLink == null) {
                return GatewayPaymentObservation.builder()
                        .orderCode(orderCode)
                        .status(GatewayPaymentObservation.GatewayStatus.UNKNOWN)
                        .build();
            }

            GatewayPaymentObservation.GatewayStatus status;
            if (paymentLink.getStatus() == PaymentLinkStatus.PAID) {
                status = GatewayPaymentObservation.GatewayStatus.PAID;
            } else if (paymentLink.getStatus() == PaymentLinkStatus.PENDING) {
                status = GatewayPaymentObservation.GatewayStatus.PENDING;
            } else if (paymentLink.getStatus() == PaymentLinkStatus.CANCELLED) {
                status = GatewayPaymentObservation.GatewayStatus.CANCELLED;
            } else if (paymentLink.getStatus() == PaymentLinkStatus.EXPIRED) {
                status = GatewayPaymentObservation.GatewayStatus.EXPIRED;
            } else {
                status = GatewayPaymentObservation.GatewayStatus.UNKNOWN;
            }

            BigDecimal amount = paymentLink.getAmountPaid() != null
                    ? BigDecimal.valueOf(paymentLink.getAmountPaid())
                    : null;

            return GatewayPaymentObservation.builder()
                    .orderCode(orderCode)
                    .status(status)
                    .amount(amount)
                    .currency("VND")
                    .gatewayPaymentLinkId(paymentLink.getId())
                    .build();
        } catch (Exception e) {
            log.error("Network or gateway exception while reconciling orderCode {}: {}", orderCode, e.getMessage());
            return GatewayPaymentObservation.builder()
                    .orderCode(orderCode)
                    .status(GatewayPaymentObservation.GatewayStatus.UNKNOWN)
                    .build();
        }
    }
}
