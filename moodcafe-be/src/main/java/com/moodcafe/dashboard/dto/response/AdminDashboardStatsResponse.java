package com.moodcafe.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsResponse {

    private StoreStats stores;
    private TagStats tags;
    private ReviewReportStats reviewReports;
    private SubscriptionStats subscriptions;
    private UserStats users;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoreStats {
        private long total;
        private long active;
        private long pending;
        private long inactive;
        private long rejected;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TagStats {
        private long pendingRequests;
        private long totalTags;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewReportStats {
        private long pending;
        private long total;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionStats {
        private long totalSubscribedOwners;
        private BigDecimal totalRevenue;
        private BigDecimal monthlyRevenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        private long total;
        private long merchants;
        private long customers;
    }
}
