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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_wallet_transactions", indexes = {
        @Index(name = "idx_merchant_tx_wallet", columnList = "wallet_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * PENDING, AVAILABLE, LOCKED
     */
    @Column(name = "balance_type", nullable = false, length = 30)
    private String balanceType;

    /**
     * HOLD_PENDING, RELEASE_TO_AVAILABLE, LOCK_FOR_WITHDRAWAL, WITHDRAWAL_PAID, WITHDRAWAL_CANCELLED, REVERSAL
     */
    @Column(name = "tx_type", nullable = false, length = 50)
    private String txType;

    /**
     * RESERVATION, WITHDRAWAL
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
