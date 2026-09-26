package com.moodcafe.subscription.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.subscription.entity.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_subscriptions",
        indexes = {
                @Index(name = "idx_user_subscriptions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_subscriptions_status", columnList = "status"),
                @Index(name = "idx_user_subscriptions_end_date", columnList = "end_date")
        }
)
@SQLDelete(sql = "UPDATE user_subscriptions SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE user_subscription_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_subscription_id", nullable = false, updatable = false)
    private UUID userSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_subscriptions_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_subscriptions_plan"))
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Builder.Default
    @Column(name = "monthly_free_sponsored_used", nullable = false)
    private Integer monthlyFreeSponsoredUsed = 0;

    @Builder.Default
    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = false;

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
