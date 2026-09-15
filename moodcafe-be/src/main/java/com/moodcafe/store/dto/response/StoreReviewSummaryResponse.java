package com.moodcafe.store.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReviewSummaryResponse {

    private UUID storeId;
    private Double averageRating;
    private Long totalReviews;
    private Double averageQuietness;
    private Double averageLighting;
    private Double averageSeating;
    private Double averageOutlet;
}
