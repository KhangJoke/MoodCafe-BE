package com.moodcafe.voucher.entity;

import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revenue_voucher_campaigns", indexes = {
        @Index(name = "idx_rev_voucher_campaigns_status", columnList = "status, created_at"),
        @Index(name = "idx_rev_voucher_campaigns_source", columnList = "funding_source, status"),
        @Index(name = "idx_rev_voucher_campaigns_dist", columnList = "distribution_type, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueVoucherCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_id", nullable = false, updatable = false)
    private UUID campaignId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_type", nullable = false, length = 30)
    private RevenueVoucherDistributionType distributionType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "funding_source", nullable = false, length = 30)
    private RevenueFundingSource fundingSource = RevenueFundingSource.ALL;

    @Column(name = "face_value_vnd", nullable = false, precision = 12, scale = 2)
    private BigDecimal faceValueVnd;

    @Builder.Default
    @Column(name = "min_gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minGrossAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "valid_days", nullable = false)
    private Integer validDays = 30;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Builder.Default
    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity = 0;

    @Builder.Default
    @Column(name = "max_claims_per_user", nullable = false)
    private Integer maxClaimsPerUser = 1;

    @Builder.Default
    @Column(name = "allocated_budget_vnd", nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedBudgetVnd = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "released_budget_vnd", nullable = false, precision = 14, scale = 2)
    private BigDecimal releasedBudgetVnd = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "exclude_from_finance", nullable = false)
    private Boolean excludeFromFinance = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private RevenueCampaignStatus status = RevenueCampaignStatus.ACTIVE;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BigDecimal getEffectiveExpenseVnd() {
        if (Boolean.TRUE.equals(excludeFromFinance)) {
            return BigDecimal.ZERO;
        }
        BigDecimal allocated = allocatedBudgetVnd != null ? allocatedBudgetVnd : BigDecimal.ZERO;
        BigDecimal released = releasedBudgetVnd != null ? releasedBudgetVnd : BigDecimal.ZERO;
        return allocated.subtract(released).max(BigDecimal.ZERO);
    }

    public int getRemainingQuantity() {
        int total = totalQuantity != null ? totalQuantity : 0;
        int issued = issuedQuantity != null ? issuedQuantity : 0;
        return Math.max(0, total - issued);
    }
}

