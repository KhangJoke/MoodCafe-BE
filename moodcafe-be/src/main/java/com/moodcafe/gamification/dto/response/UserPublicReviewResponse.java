package com.moodcafe.gamification.dto.response;

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
public class UserPublicReviewResponse {
    private UUID reviewId;
    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String storeImageUrl;
    private BigDecimal overallRating;
    private String content;
    private List<String> imageUrls;
    private boolean verified;
    private Instant createdAt;
}
