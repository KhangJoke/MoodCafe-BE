package com.moodcafe.subscription.entity;

import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "subscription_plans",
        indexes = {
                @Index(name = "idx_subscription_plans_name", columnList = "name"),
                @Index(name = "idx_subscription_plans_plan_code", columnList = "plan_code")
        }
)
@SQLDelete(sql = "UPDATE subscription_plans SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE subscription_plan_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", length = 50)
    private SubscriptionPlanCode planCode;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "duration_days")
    private Integer durationDays = 30;

    @Builder.Default
    @Column(name = "max_branches", nullable = false)
    private Integer maxBranches = 1;

    @Builder.Default
    @Column(name = "table_management", nullable = false)
    private boolean tableManagement = false;

    @Builder.Default
    @Column(name = "advanced_analytics", nullable = false)
    private boolean advancedAnalytics = false;

    @Builder.Default
    @Column(name = "deposit_rules", nullable = false)
    private boolean depositRules = false;

    @Builder.Default
    @Column(name = "allow_sponsored_listing", nullable = false)
    private boolean allowSponsoredListing = false;

    @Builder.Default
    @Column(name = "vip_hero_banner", nullable = false)
    private boolean vipHeroBanner = false;

    @Builder.Default
    @Column(name = "qr_table_menu", nullable = false)
    private boolean qrTableMenu = false;

    @Builder.Default
    @Column(name = "ai_recommendation", nullable = false)
    private boolean aiRecommendation = false;

    @Builder.Default
    @Column(name = "monthly_free_sponsored_count", nullable = false)
    private Integer monthlyFreeSponsoredCount = 0;

    @Builder.Default
    @Column(name = "dedicated_support", nullable = false)
    private boolean dedicatedSupport = false;

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
