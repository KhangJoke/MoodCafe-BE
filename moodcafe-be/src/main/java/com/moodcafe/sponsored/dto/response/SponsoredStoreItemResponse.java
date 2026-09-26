package com.moodcafe.sponsored.dto.response;

import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredStoreItemResponse {

    private UUID campaignId;
    private UUID storeId;
    private String name;
    private String address;
    private String district;
    private String coverImageUrl;
    private String customBannerUrl;
    private String campaignTitle;
    private Double averageRating;
    private Long reviewCount;
    private Long priceFrom;
    private Long priceTo;
    @Builder.Default
    private boolean sponsored = true;
    private SponsoredPlacement sponsoredPlacement;
    @Builder.Default
    private String badgeText = "Sponsored";
}
