package com.moodcafe.sponsored.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredAdminStatsResponse {

    private long totalCampaigns;
    private long activeCampaigns;
    private long pendingCampaigns;
    private long expiredCampaigns;
    private long cancelledCampaigns;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private Map<String, Long> activeCountByPlacement;
}
