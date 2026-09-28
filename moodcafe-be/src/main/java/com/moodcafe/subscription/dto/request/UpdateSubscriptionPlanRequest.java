package com.moodcafe.subscription.dto.request;

import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubscriptionPlanRequest {

    private String name;

    private SubscriptionPlanCode planCode;

    private String displayName;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    private Integer durationDays;

    private Integer maxBranches;

    private Boolean tableManagement;

    private Boolean advancedAnalytics;

    private Boolean depositRules;

    private Boolean allowSponsoredListing;

    private Boolean vipHeroBanner;

    private Boolean qrTableMenu;

    private Boolean aiRecommendation;

    private Integer monthlyFreeSponsoredCount;

    private Boolean dedicatedSupport;

    private Boolean active;
}
