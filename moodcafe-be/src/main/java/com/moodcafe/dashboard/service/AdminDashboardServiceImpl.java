package com.moodcafe.dashboard.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.dashboard.abstraction.service.AdminDashboardService;
import com.moodcafe.dashboard.dto.response.AdminDashboardStatsResponse;
import com.moodcafe.store.abstraction.repository.ReviewReportRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.repository.UserSubscriptionRepository;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final StoreRepository storeRepository;
    private final TagRepository tagRepository;
    private final StoreTagRepository storeTagRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getAdminDashboardStats() {
        // 1. Stores stats
        long storeTotal = storeRepository.count();
        long storeActive = 0;
        long storePending = 0;
        long storeInactive = 0;
        long storeRejected = 0;

        List<Object[]> storeCounts = storeRepository.countGroupedByStatus();
        for (Object[] row : storeCounts) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                StoreStatus st = (StoreStatus) row[0];
                long count = ((Number) row[1]).longValue();
                if (StoreStatus.ACTIVE.equals(st)) {
                    storeActive = count;
                } else if (StoreStatus.PENDING.equals(st)) {
                    storePending = count;
                } else if (StoreStatus.INACTIVE.equals(st)) {
                    storeInactive = count;
                } else if (StoreStatus.REJECTED.equals(st)) {
                    storeRejected = count;
                }
            }
        }

        AdminDashboardStatsResponse.StoreStats storeStats = AdminDashboardStatsResponse.StoreStats.builder()
                .total(storeTotal)
                .active(storeActive)
                .pending(storePending)
                .inactive(storeInactive)
                .rejected(storeRejected)
                .build();

        // 2. Tags stats
        long pendingTagRequests = storeTagRepository.countByStatus(StoreTagStatus.PENDING);
        long totalTags = tagRepository.count();

        AdminDashboardStatsResponse.TagStats tagStats = AdminDashboardStatsResponse.TagStats.builder()
                .pendingRequests(pendingTagRequests)
                .totalTags(totalTags)
                .build();

        // 3. Review reports stats
        long reviewReportsTotal = reviewReportRepository.count();
        long reviewReportsPending = reviewReportRepository.countByStatus(ReviewReportStatus.PENDING);

        AdminDashboardStatsResponse.ReviewReportStats reviewReportStats = AdminDashboardStatsResponse.ReviewReportStats.builder()
                .pending(reviewReportsPending)
                .total(reviewReportsTotal)
                .build();

        // 4. Subscriptions stats
        long totalSubscribedOwners = userSubscriptionRepository.countDistinctUsersWithSubscriptions();
        BigDecimal totalRevenue = subscriptionPaymentRepository.sumTotalRevenue();
        Instant startOfMonth = Instant.now().minus(30, ChronoUnit.DAYS);
        BigDecimal monthlyRevenue = subscriptionPaymentRepository.sumRevenueSince(startOfMonth);

        AdminDashboardStatsResponse.SubscriptionStats subscriptionStats = AdminDashboardStatsResponse.SubscriptionStats.builder()
                .totalSubscribedOwners(totalSubscribedOwners)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();

        // 5. Users stats
        long totalUsers = userRepository.count();
        long merchants = 0;
        long customers = 0;

        List<Object[]> userCounts = userRepository.countGroupedByRoleName();
        for (Object[] row : userCounts) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                String roleName = ((String) row[0]).trim().toUpperCase();
                long count = ((Number) row[1]).longValue();
                if ("MERCHANT".equals(roleName) || "MERCHANT_STAFF".equals(roleName)) {
                    merchants += count;
                } else if ("CUSTOMER".equals(roleName)) {
                    customers += count;
                }
            }
        }

        AdminDashboardStatsResponse.UserStats userStats = AdminDashboardStatsResponse.UserStats.builder()
                .total(totalUsers)
                .merchants(merchants)
                .customers(customers)
                .build();

        return AdminDashboardStatsResponse.builder()
                .stores(storeStats)
                .tags(tagStats)
                .reviewReports(reviewReportStats)
                .subscriptions(subscriptionStats)
                .users(userStats)
                .build();
    }
}
