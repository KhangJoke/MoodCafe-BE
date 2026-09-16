package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReviewResponse {

    private UUID reviewId;
    private UUID storeId;
    private String storeName;
    private UUID userId;
    private String userFullName;
    private String userAvatarUrl;
    private UUID visitVerificationId;
    private BigDecimal overallRating;
    private Integer quietnessRating;
    private Integer lightingRating;
    private Integer seatingRating;
    private Integer outletRating;
    private String content;
    private List<String> imageUrls;
    private Instant createdAt;
    private Instant updatedAt;
}
