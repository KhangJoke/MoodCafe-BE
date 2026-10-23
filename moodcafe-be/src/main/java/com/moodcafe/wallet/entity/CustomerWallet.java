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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_wallets", indexes = {
        @Index(name = "idx_customer_wallets_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "points_balance", nullable = false)
    @Builder.Default
    private Long pointsBalance = 0L;

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
