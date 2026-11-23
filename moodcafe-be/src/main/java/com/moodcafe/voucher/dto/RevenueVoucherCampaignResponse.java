package com.moodcafe.voucher.dto;

import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueVoucherCampaignResponse {
    private UUID campaignId;
    private String title;
    private String description;
    private RevenueVoucherDistributionType distributionType;
    private RevenueFundingSource fundingSource;
    private BigDecimal faceValueVnd;
    private BigDecimal minGrossAmount;
    private Integer validDays;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer remainingQuantity;
    private Long usedQuantity;
    private Integer maxClaimsPerUser;
    private BigDecimal allocatedBudgetVnd;
    private BigDecimal releasedBudgetVnd;
    private BigDecimal effectiveExpenseVnd;
    private RevenueCampaignStatus status;
    private Instant startsAt;
    private Instant endsAt;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}

