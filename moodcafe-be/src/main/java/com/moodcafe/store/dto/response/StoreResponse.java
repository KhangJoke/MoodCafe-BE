package com.moodcafe.store.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.tag.dto.response.StoreTagResponse;
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
public class StoreResponse {

    private UUID storeId;
    private String name;
    private String description;
    private String address;
    private String district;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String priceRange;
    private Long priceFrom;
    private Long priceTo;
    private String phone;
    private String email;
    private StoreStatus status;
    private List<StoreImageResponse> images;
    private List<StoreTagResponse> tags;
    private List<StoreReviewResponse> reviews;
    private StoreReviewSummaryResponse reviewSummary;
    private Double overallRating;
    private Long reviewCount;

    @JsonAlias({"hasUserReviewed"})
    private Boolean hasReviewed;

    @JsonAlias({"myReview"})
    private StoreReviewResponse userReview;

    private Instant createdAt;
    private Instant updatedAt;

    @JsonProperty("hasUserReviewed")
    public Boolean getHasUserReviewed() {
        return hasReviewed;
    }

    @JsonProperty("myReview")
    public StoreReviewResponse getMyReview() {
        return userReview;
    }
}
