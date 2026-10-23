package com.moodcafe.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "purpose", nullable = false)
    private String purpose; // BOOKING, SUBSCRIPTION

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Builder.Default
    @Column(name = "business_type", length = 50)
    private String businessType = "BOOKING";

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, PAID, CANCELLED, EXPIRED, UNKNOWN

    @Column(name = "gateway_payment_link_id")
    private String gatewayPaymentLinkId;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "intent_key")
    private String intentKey;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
