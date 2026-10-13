package com.moodcafe.sponsored.entity;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sponsored_plans",
        indexes = {
                @Index(name = "idx_sponsored_plans_name", columnList = "name"),
                @Index(name = "idx_sponsored_plans_placement", columnList = "placement"),
                @Index(name = "idx_sponsored_plans_active", columnList = "is_active")
        }
)
@SQLDelete(sql = "UPDATE sponsored_plans SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE sponsored_plan_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sponsored_plan_id", nullable = false, updatable = false)
    private UUID sponsoredPlanId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", nullable = false, length = 50)
    private SponsoredPlacement placement;

    @Builder.Default
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 7;

    @Builder.Default
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "badge_text", length = 50)
    private String badgeText;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
