package com.moodcafe.sponsored.dto.response;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
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
public class SponsoredPlacementPriceInfo {

    private SponsoredPlacement placement;
    private String displayName;
    private String description;
    private BigDecimal weeklyPrice;
    private BigDecimal monthlyPrice;
    private int monthlySavingsPercent;
    private boolean recommended;
}
