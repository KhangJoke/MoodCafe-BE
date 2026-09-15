package com.moodcafe.store.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
