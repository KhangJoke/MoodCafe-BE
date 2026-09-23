package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDashboardResponse {

    private MerchantStoreSummary store;
    private MerchantMetrics metrics;

    @Builder.Default
    private List<MerchantRecentSnap> recentSnaps = new ArrayList<>();

    @Builder.Default
    private List<MerchantRecentReview> recentReviews = new ArrayList<>();

    @Builder.Default
    private List<StoreTagResponse> tags = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantStoreSummary {
        private UUID storeId;
        private String name;
        private String address;
        private String district;
        private StoreStatus status;
        private String coverImageUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantMetrics {
        private long totalSnaps;
        private long totalReviews;
        private double averageRating;
        private long activeTagsCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantRecentSnap {
        private UUID visitVerificationId;
        private String imageUrl;
        private UUID userId;
        private String userFullName;
        private String userAvatarUrl;
        private Instant capturedAt;
        private BigDecimal distanceFromStoreMeters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MerchantRecentReview {
        private UUID reviewId;
        private UUID userId;
        private String userFullName;
        private String userAvatarUrl;
        private BigDecimal overallRating;
        private String content;
        private List<String> imageUrls;
        private String merchantReply;
        private Instant replyAt;
        private Instant createdAt;
    }
}
