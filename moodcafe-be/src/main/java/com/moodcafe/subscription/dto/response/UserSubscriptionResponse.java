package com.moodcafe.subscription.dto.response;

import com.moodcafe.subscription.entity.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionResponse {

    private UUID userSubscriptionId;
    private UUID userId;
    private SubscriptionPlanResponse plan;
    private Instant startDate;
    private Instant endDate;
    private SubscriptionStatus status;
    private Long remainingDays;
    private Integer currentBranchCount;
    private Integer maxBranches;
    private Integer monthlyFreeSponsoredUsed;
    private Integer monthlyFreeSponsoredRemaining;
    private boolean autoRenew;
}
