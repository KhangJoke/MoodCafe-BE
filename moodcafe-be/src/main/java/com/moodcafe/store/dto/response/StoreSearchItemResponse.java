package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSearchItemResponse {

    private UUID storeId;
    private String name;
    private String description;
    private String address;
    private String district;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String priceRange;
    private Long priceFrom;
    private Long priceTo;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean isOpenNow;
    private Double overallRating;
    private Long reviewCount;
    private Long favoriteCount;
    private Integer matchScore;
    private String primaryImageUrl;
    private List<StoreSearchTagItem> highlightTags;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreSearchTagItem {
        private UUID tagId;
        private String name;
        private String categoryCode;
        private String categoryName;
        private Integer scaleValue;
    }
}
