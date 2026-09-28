package com.moodcafe.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatisticsResponse {

    private long totalSubscribedOwners;
    private long activeSubscriptions;
    private long expiredSubscriptions;
    private long pendingSubscriptions;
    private long cancelledSubscriptions;

    private long newSubscriptionsCount;
    private long renewalSubscriptionsCount;

    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal todayRevenue;
    private long totalSuccessfulPayments;

    private List<PlanSubscriptionStatsResponse> planStats;
}
