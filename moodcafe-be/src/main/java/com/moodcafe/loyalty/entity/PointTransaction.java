package com.moodcafe.loyalty.entity;

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
@Table(name = "customer_point_transactions", indexes = {
        @Index(name = "idx_customer_point_tx_user", columnList = "user_id"),
        @Index(name = "idx_customer_point_tx_bizkey", columnList = "business_key", unique = true)
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

    @Column(name = "business_key", nullable = false, unique = true)
    private String businessKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private Long amount; // Positive = credit, Negative = debit

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "tx_type", nullable = false, length = 30)
    private String txType; // REFUND_CREDIT, REWARD_CREDIT, REDEEM_DEBIT, EXPIRE_DEBIT

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

