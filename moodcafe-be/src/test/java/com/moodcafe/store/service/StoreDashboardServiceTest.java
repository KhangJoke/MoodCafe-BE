package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.response.MerchantDashboardResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.VisitVerification;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.store.mapper.StoreImageMapper;
import com.moodcafe.store.mapper.StoreMapper;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.tag.mapper.StoreTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreDashboardServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreRoleRepository storeRoleRepository;
    @Mock
    private StoreStaffRepository storeStaffRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private StoreTagRepository storeTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagCategoryRepository tagCategoryRepository;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private StoreImageMapper storeImageMapper;
    @Mock
    private StoreTagMapper storeTagMapper;
    @Mock
    private StoreStaffService storeStaffService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private FavoriteStoreRepository favoriteStoreRepository;
    @Mock
    private StoreReviewRepository storeReviewRepository;
    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    @Mock
    private SystemConfigurationService configurationService;
    @Mock
    private StoreReviewMapper storeReviewMapper;
    @Mock
    private TagRatingRepository tagRatingRepository;
    @Mock
    private VisitVerificationRepository visitVerificationRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private com.moodcafe.store.abstraction.repository.StoreScheduleRepository storeScheduleRepository;
    @Mock
    private com.moodcafe.store.mapper.StoreScheduleMapper storeScheduleMapper;

    @InjectMocks
    private StoreServiceImpl storeService;

    private UUID storeId;
    private Store mockStore;
    private User mockUser;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        mockStore = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Da Nang")
                .address("123 Bach Dang, Hai Chau, Da Nang")
                .status(StoreStatus.ACTIVE)
                .latitude(new BigDecimal("16.0678"))
                .longitude(new BigDecimal("108.2208"))
                .build();

        mockUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .avatarUrl("https://example.com/avatar.jpg")
                .email("test@moodcafe.com")
                .build();
    }

    @Test
    @DisplayName("Test 1: Valid owner/manager -> returns MerchantDashboardResponse with summary, metrics, snaps & reviews capped at 5")
    void getMerchantDashboard_ValidOwnerOrManager_Success() {
        // Arrange
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));

        StoreImage primaryImage = StoreImage.builder()
                .store(mockStore)
                .imageUrl("https://example.com/cover.jpg")
                .primary(true)
                .build();
        when(storeImageRepository.findAllByStoreStoreId(storeId)).thenReturn(List.of(primaryImage));

        when(visitVerificationRepository.countByStoreStoreId(storeId)).thenReturn(8L);
        when(storeReviewRepository.countByStoreStoreId(storeId)).thenReturn(12L);

        // Average rating calculation: 4.66 -> rounded to 4.7
        when(storeReviewRepository.getReviewSummaryByStoreId(storeId))
                .thenReturn(Collections.singletonList(new Object[]{4.66, 4.5, 4.8, 4.0, 4.5, 12L}));

        Tag tag = Tag.builder().tagId(UUID.randomUUID()).name("Yên tĩnh").build();
        StoreTag approvedTag = StoreTag.builder()
                .storeId(storeId)
                .tag(tag)
                .status(StoreTagStatus.APPROVED)
                .build();
        StoreTag pendingTag = StoreTag.builder()
                .storeId(storeId)
                .tag(tag)
                .status(StoreTagStatus.PENDING)
                .build();
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(approvedTag, pendingTag));
        when(storeTagMapper.toResponse(approvedTag)).thenReturn(
                StoreTagResponse.builder().tagId(tag.getTagId()).tagName(tag.getName()).build()
        );

        // Prepare 7 snaps to verify limiting to max 5
        List<VisitVerification> snapsList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            snapsList.add(VisitVerification.builder()
                    .visitVerificationId(UUID.randomUUID())
                    .store(mockStore)
                    .user(mockUser)
                    .imageUrl("https://example.com/snap" + i + ".jpg")
                    .capturedAt(Instant.now())
                    .distanceFromStoreMeters(new BigDecimal("15.5"))
                    .build());
        }
        when(visitVerificationRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId))
                .thenReturn(snapsList);

        // Prepare 7 reviews to verify limiting to max 5
        List<StoreReview> reviewsList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            reviewsList.add(StoreReview.builder()
                    .reviewId(UUID.randomUUID())
                    .store(mockStore)
                    .user(mockUser)
                    .overallRating(new BigDecimal("4.5"))
                    .content("Review content " + i)
                    .images(List.of())
                    .createdAt(Instant.now())
                    .build());
        }
        when(storeReviewRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId))
                .thenReturn(reviewsList);

        // Act
        MerchantDashboardResponse response = storeService.getMerchantDashboard(storeId);

        // Assert
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        assertThat(response).isNotNull();

        // Summary
        assertThat(response.getStore()).isNotNull();
        assertThat(response.getStore().getStoreId()).isEqualTo(storeId);
        assertThat(response.getStore().getName()).isEqualTo("Mood Cafe Da Nang");
        assertThat(response.getStore().getAddress()).isEqualTo("123 Bach Dang, Hai Chau, Da Nang");
        assertThat(response.getStore().getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");

        // Metrics
        assertThat(response.getMetrics()).isNotNull();
        assertThat(response.getMetrics().getTotalSnaps()).isEqualTo(8L);
        assertThat(response.getMetrics().getTotalReviews()).isEqualTo(12L);
        assertThat(response.getMetrics().getAverageRating()).isEqualTo(4.7);
        assertThat(response.getMetrics().getActiveTagsCount()).isEqualTo(1L);

        // Recent snaps: capped at 5
        assertThat(response.getRecentSnaps()).hasSize(5);
        MerchantDashboardResponse.MerchantRecentSnap firstSnap = response.getRecentSnaps().get(0);
        assertThat(firstSnap.getUserFullName()).isEqualTo("Nguyen Van A");
        assertThat(firstSnap.getUserAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");

        // Recent reviews: capped at 5
        assertThat(response.getRecentReviews()).hasSize(5);
        MerchantDashboardResponse.MerchantRecentReview firstReview = response.getRecentReviews().get(0);
        assertThat(firstReview.getUserFullName()).isEqualTo("Nguyen Van A");
        assertThat(firstReview.getUserAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(firstReview.getOverallRating()).isEqualTo(new BigDecimal("4.5"));

        // Tags
        assertThat(response.getTags()).hasSize(2);
    }

    @Test
    @DisplayName("Test 2: Store has no snaps or reviews -> returns 0 metrics, 0.0 rating and empty lists without NPE")
    void getMerchantDashboard_EmptySnapsAndReviews_ReturnsZeroMetricsAndEmptyLists() {
        // Arrange
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(storeImageRepository.findAllByStoreStoreId(storeId)).thenReturn(List.of());

        when(visitVerificationRepository.countByStoreStoreId(storeId)).thenReturn(0L);
        when(storeReviewRepository.countByStoreStoreId(storeId)).thenReturn(0L);
        when(storeReviewRepository.getReviewSummaryByStoreId(storeId)).thenReturn(Collections.emptyList());
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(Collections.emptyList());

        when(visitVerificationRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId))
                .thenReturn(Collections.emptyList());
        when(storeReviewRepository.findTop5ByStoreStoreIdOrderByCreatedAtDesc(storeId))
                .thenReturn(Collections.emptyList());

        // Act
        MerchantDashboardResponse response = storeService.getMerchantDashboard(storeId);

        // Assert
        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        assertThat(response).isNotNull();

        assertThat(response.getMetrics()).isNotNull();
        assertThat(response.getMetrics().getTotalSnaps()).isZero();
        assertThat(response.getMetrics().getTotalReviews()).isZero();
        assertThat(response.getMetrics().getAverageRating()).isEqualTo(0.0);
        assertThat(response.getMetrics().getActiveTagsCount()).isZero();

        assertThat(response.getRecentSnaps()).isEmpty();
        assertThat(response.getRecentReviews()).isEmpty();
        assertThat(response.getTags()).isEmpty();
    }

    @Test
    @DisplayName("Test 3: User without OWNER or MANAGER access -> throws FORBIDDEN_STORE_ACCESS")
    void getMerchantDashboard_AccessDenied_ThrowsAppException() {
        // Arrange
        doThrow(new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS))
                .when(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");

        // Act & Assert
        assertThatThrownBy(() -> storeService.getMerchantDashboard(storeId))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN_STORE_ACCESS));

        verify(storeRepository, never()).findById(any());
        verify(storeReviewRepository, never()).countByStoreStoreId(any());
    }

    @Test
    @DisplayName("Test 4: Store not found -> throws STORE_NOT_FOUND")
    void getMerchantDashboard_StoreNotFound_ThrowsAppException() {
        // Arrange
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> storeService.getMerchantDashboard(storeId))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STORE_NOT_FOUND));

        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(storeReviewRepository, never()).countByStoreStoreId(any());
    }
}
