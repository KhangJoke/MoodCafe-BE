package com.moodcafe.subscription.dto.request;

import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateSubscriptionPlanRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    private SubscriptionPlanCode planCode;

    @NotBlank(message = "Tên hiển thị không được để trống")
    private String displayName;

    private String description;

    @NotNull(message = "Giá gói không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Builder.Default
    private Integer durationDays = 30;

    @Builder.Default
    private Integer maxBranches = 1;

    @Builder.Default
    private Boolean tableManagement = false;

    @Builder.Default
    private Boolean advancedAnalytics = false;

    @Builder.Default
    private Boolean depositRules = false;

    @Builder.Default
    private Boolean allowSponsoredListing = false;

    @Builder.Default
    private Boolean vipHeroBanner = false;

    @Builder.Default
    private Boolean qrTableMenu = false;

    @Builder.Default
    private Boolean aiRecommendation = false;

    @Builder.Default
    private Integer monthlyFreeSponsoredCount = 0;

    @Builder.Default
    private Boolean dedicatedSupport = false;

    @Builder.Default
    private Boolean active = true;
}
