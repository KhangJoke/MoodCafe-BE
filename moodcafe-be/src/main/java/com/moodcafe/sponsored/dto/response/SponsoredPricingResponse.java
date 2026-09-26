package com.moodcafe.sponsored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredPricingResponse {

    private boolean eligible;
    private String userPlanCode;
    private String userPlanName;
    private String warningBannerMessage;
    private boolean freeQuotaAvailable;
    private int freeQuotaRemaining;
    private List<SponsoredPlacementPriceInfo> placements;
}
