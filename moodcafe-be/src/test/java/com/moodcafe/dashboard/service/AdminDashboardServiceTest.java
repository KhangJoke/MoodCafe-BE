package com.moodcafe.dashboard.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private StoreTagRepository storeTagRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    @DisplayName("getAdminDashboardStats computes all platform KPIs accurately")
    void getAdminDashboardStats_Success() {
        // Store mocks
        when(storeRepository.count()).thenReturn(120L);
        when(storeRepository.countGroupedByStatus()).thenReturn(List.<Object[]>of(
                new Object[]{StoreStatus.ACTIVE, 95L},
                new Object[]{StoreStatus.PENDING, 15L},
                new Object[]{StoreStatus.INACTIVE, 7L},
                new Object[]{StoreStatus.REJECTED, 3L}
        ));

        // Tag mocks
        when(storeTagRepository.countByStatus(StoreTagStatus.PENDING)).thenReturn(12L);
        when(tagRepository.count()).thenReturn(48L);

        // Review report mocks
        when(reviewReportRepository.count()).thenReturn(45L);
        when(reviewReportRepository.countByStatus(ReviewReportStatus.PENDING)).thenReturn(8L);

        // Subscription mocks
        when(userSubscriptionRepository.countDistinctUsersWithSubscriptions()).thenReturn(35L);
        when(subscriptionPaymentRepository.sumTotalRevenue()).thenReturn(new BigDecimal("24500000"));
        when(subscriptionPaymentRepository.sumRevenueSince(any(Instant.class))).thenReturn(new BigDecimal("8200000"));

        // User mocks
        when(userRepository.count()).thenReturn(1250L);
        when(userRepository.countGroupedByRoleName()).thenReturn(List.<Object[]>of(
                new Object[]{"CUSTOMER", 1130L},
                new Object[]{"MERCHANT_STAFF", 120L}
        ));

        AdminDashboardStatsResponse result = adminDashboardService.getAdminDashboardStats();

        assertThat(result).isNotNull();
        // Verify stores
        assertThat(result.getStores().getTotal()).isEqualTo(120);
        assertThat(result.getStores().getActive()).isEqualTo(95);
        assertThat(result.getStores().getPending()).isEqualTo(15);
        assertThat(result.getStores().getInactive()).isEqualTo(7);
        assertThat(result.getStores().getRejected()).isEqualTo(3);

        // Verify tags
        assertThat(result.getTags().getPendingRequests()).isEqualTo(12);
        assertThat(result.getTags().getTotalTags()).isEqualTo(48);

        // Verify review reports
        assertThat(result.getReviewReports().getTotal()).isEqualTo(45);
        assertThat(result.getReviewReports().getPending()).isEqualTo(8);

        // Verify subscriptions
        assertThat(result.getSubscriptions().getTotalSubscribedOwners()).isEqualTo(35);
        assertThat(result.getSubscriptions().getTotalRevenue()).isEqualTo(new BigDecimal("24500000"));
        assertThat(result.getSubscriptions().getMonthlyRevenue()).isEqualTo(new BigDecimal("8200000"));

        // Verify users
        assertThat(result.getUsers().getTotal()).isEqualTo(1250);
        assertThat(result.getUsers().getMerchants()).isEqualTo(120);
        assertThat(result.getUsers().getCustomers()).isEqualTo(1130);
    }
}
