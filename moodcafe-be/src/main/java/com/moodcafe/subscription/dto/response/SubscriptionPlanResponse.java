package com.moodcafe.subscription.dto.response;

import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {

    private UUID subscriptionPlanId;
    private String name;
    private SubscriptionPlanCode planCode;
    private String displayName;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Integer maxBranches;
    private boolean tableManagement;
    private boolean advancedAnalytics;
    private boolean depositRules;
    private boolean allowSponsoredListing;
    private boolean vipHeroBanner;
    private boolean qrTableMenu;
    private boolean aiRecommendation;
    private Integer monthlyFreeSponsoredCount;
    private boolean dedicatedSupport;
    private List<String> features;
}
