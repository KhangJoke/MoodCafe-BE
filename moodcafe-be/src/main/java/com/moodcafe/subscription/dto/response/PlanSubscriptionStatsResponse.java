package com.moodcafe.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanSubscriptionStatsResponse {

    private UUID subscriptionPlanId;
    private String name;
    private String displayName;
    private BigDecimal price;
    private long subscriberCount;
    private BigDecimal totalRevenue;
}
