package com.moodcafe.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_withdrawals", indexes = {
        @Index(name = "idx_merchant_withdrawals_store", columnList = "store_id, status"),
        @Index(name = "idx_merchant_withdrawals_status", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "withdrawal_id", nullable = false, updatable = false)
    private UUID withdrawalId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Snapshot bank account details at request time
    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    /**
     * REQUESTED, PROCESSING, PAID, REJECTED, RECONCILIATION_REQUIRED, CANCELLED
     */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "claimed_by")
    private UUID claimedBy;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "transaction_reference", length = 150)
    private String transactionReference;

    @Column(name = "evidence_image_url", length = 500)
    private String evidenceImageUrl;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "resolution_status", length = 50)
    private String resolutionStatus;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
