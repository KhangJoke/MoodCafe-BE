package com.moodcafe.store.dto.response;

import com.moodcafe.store.dto.response.StoreSearchItemResponse.StoreSearchTagItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStoreResponse {

    private UUID favoriteStoreId;
    private UUID userId;
    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String district;
    private String primaryImageUrl;
    private String priceRange;
    private Long priceFrom;
    private Long priceTo;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean isOpenNow;
    private Double overallRating;
    private Long reviewCount;
    private List<StoreSearchTagItem> highlightTags;
    private Instant createdAt;
}

