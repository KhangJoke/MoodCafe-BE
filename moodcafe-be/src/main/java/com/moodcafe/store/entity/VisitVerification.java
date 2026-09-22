package com.moodcafe.store.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.store.entity.enums.VisitVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "visit_verifications",
        indexes = {
                @Index(name = "idx_visit_verifications_user_id", columnList = "user_id"),
                @Index(name = "idx_visit_verifications_store_id", columnList = "store_id"),
                @Index(name = "idx_visit_verifications_status", columnList = "status"),
                @Index(name = "idx_visit_verifications_captured_at", columnList = "captured_at")
        }
)
@SQLDelete(sql = "UPDATE visit_verifications SET is_deleted = true WHERE visit_verification_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "visit_verification_id", nullable = false, updatable = false)
    private UUID visitVerificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_visit_verifications_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_visit_verifications_store"))
    private Store store;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "distance_from_store_meters", precision = 8, scale = 2)
    private BigDecimal distanceFromStoreMeters;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private VisitVerificationStatus status = VisitVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
