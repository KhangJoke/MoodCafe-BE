package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantReviewStatsResponse {

    private UUID storeId;
    private long totalReviews;
    private double averageRating;
    private long unrepliedCount;
    private long repliedCount;
    private Map<Integer, Long> starCounts;
}
