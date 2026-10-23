package com.moodcafe.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_transactions", indexes = {
        @Index(name = "idx_point_transactions_user", columnList = "user_id, created_at"),
        @Index(name = "idx_point_transactions_key", columnList = "business_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "business_key", nullable = false, unique = true, length = 150)
    private String businessKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Positive for credits, negative for debits.
     */
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    /**
     * REFUND_CREDIT, REWARD_CREDIT, REDEEM_DEBIT, REWARD_REVERSAL, EXPIRED_DEBIT
     */
    @Column(name = "tx_type", nullable = false, length = 50)
    private String txType;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
