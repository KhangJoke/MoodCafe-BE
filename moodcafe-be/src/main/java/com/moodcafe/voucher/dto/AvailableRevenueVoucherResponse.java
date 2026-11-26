package com.moodcafe.voucher.dto;

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
public class AvailableRevenueVoucherResponse {
    private UUID campaignId;
    private String title;
    private String description;
    private String bannerUrl;
    private BigDecimal faceValueVnd;
    private BigDecimal minGrossAmount;
    private Integer validDays;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer remainingQuantity;
    private Integer maxClaimsPerUser;
    private Instant startsAt;
    private Instant endsAt;
    private Long myClaimedCount;
    private boolean canClaim;
}

