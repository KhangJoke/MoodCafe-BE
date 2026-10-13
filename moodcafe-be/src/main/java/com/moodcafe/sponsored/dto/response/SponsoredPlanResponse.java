package com.moodcafe.sponsored.dto.response;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredPlanResponse {

    private UUID sponsoredPlanId;
    private String name;
    private String displayName;
    private SponsoredPlacement placement;
    private String placementDisplayName;
    private Integer durationDays;
    private BigDecimal price;
    private String description;
    private String badgeText;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
