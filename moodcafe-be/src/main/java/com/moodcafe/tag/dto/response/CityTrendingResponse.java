package com.moodcafe.tag.dto.response;

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
public class CityTrendingResponse {

    private String itemType; // "TAG" or "STORE"
    private UUID tagId;
    private String tagName;
    private UUID storeId;
    private String storeName;
    private String categoryCode;
    private String categoryName;
    private String badgeText;
    private String subtitle;
    private String imageUrl;
    private long interestedCount;
    private long storeCount;
    private Double rating;
    private Long reviewCount;
}
