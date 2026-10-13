package com.moodcafe.sponsored.mapper;

import com.moodcafe.sponsored.dto.response.SponsoredPlanResponse;
import com.moodcafe.sponsored.entity.SponsoredPlan;
import org.springframework.stereotype.Component;

@Component
public class SponsoredPlanMapper {

    public SponsoredPlanResponse toResponse(SponsoredPlan plan) {
        if (plan == null) return null;

        return SponsoredPlanResponse.builder()
                .sponsoredPlanId(plan.getSponsoredPlanId())
                .name(plan.getName())
                .displayName(plan.getDisplayName())
                .placement(plan.getPlacement())
                .placementDisplayName(plan.getPlacement() != null ? plan.getPlacement().getDisplayName() : null)
                .durationDays(plan.getDurationDays())
                .price(plan.getPrice())
                .description(plan.getDescription())
                .badgeText(plan.getBadgeText())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
