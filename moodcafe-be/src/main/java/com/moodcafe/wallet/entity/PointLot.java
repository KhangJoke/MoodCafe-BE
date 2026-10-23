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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_lots", indexes = {
        @Index(name = "idx_point_lots_user_active", columnList = "user_id, source_type, remaining_points"),
        @Index(name = "idx_point_lots_expiry", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lot_id", nullable = false, updatable = false)
    private UUID lotId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Source type: REFUND (never expires) or REWARD (expires in 365 days).
     */
    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "issued_points", nullable = false)
    private Long issuedPoints;

    @Column(name = "remaining_points", nullable = false)
    private Long remainingPoints;

    /**
     * Nullable: null for REFUND (never expires); instant for REWARD.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
