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
@Table(name = "customer_point_lots", indexes = {
        @Index(name = "idx_customer_point_lots_user", columnList = "user_id"),
        @Index(name = "idx_customer_point_lots_exp", columnList = "expires_at")
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

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // REFUND or REWARD

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "issued_points", nullable = false)
    private Long issuedPoints;

    @Column(name = "remaining_points", nullable = false)
    private Long remainingPoints;

    @Column(name = "expires_at")
    private Instant expiresAt; // null for REFUND (never expires)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

