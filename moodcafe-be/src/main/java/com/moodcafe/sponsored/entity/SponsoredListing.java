package com.moodcafe.sponsored.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.sponsored.entity.enums.SponsoredDurationType;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import com.moodcafe.store.entity.Store;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sponsored_listings",
        indexes = {
                @Index(name = "idx_sponsored_store_id", columnList = "store_id"),
                @Index(name = "idx_sponsored_user_id", columnList = "user_id"),
                @Index(name = "idx_sponsored_placement", columnList = "placement"),
                @Index(name = "idx_sponsored_status", columnList = "status"),
                @Index(name = "idx_sponsored_dates", columnList = "start_date, end_date")
        }
)
@SQLDelete(sql = "UPDATE sponsored_listings SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE sponsored_listing_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sponsored_listing_id", nullable = false, updatable = false)
    private UUID sponsoredListingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sponsored_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sponsored_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", nullable = false, length = 50)
    private SponsoredPlacement placement;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false, length = 30)
    private SponsoredDurationType durationType;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Builder.Default
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_free_quota_used", nullable = false)
    private boolean freeQuotaUsed = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private SponsoredListingStatus status = SponsoredListingStatus.PENDING;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "custom_banner_url", length = 500)
    private String customBannerUrl;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "PENDING";

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
