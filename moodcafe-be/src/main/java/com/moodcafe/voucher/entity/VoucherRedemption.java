package com.moodcafe.voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voucher_redemptions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_voucher_redemptions_user_key", columnNames = {"user_id", "idempotency_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "redemption_id", nullable = false, updatable = false)
    private UUID redemptionId;

    @Column(name = "idempotency_key", nullable = false, length = 150)
    private String idempotencyKey;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
