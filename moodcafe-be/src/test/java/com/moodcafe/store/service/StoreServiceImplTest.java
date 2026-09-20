package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.FavoriteStoreRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.StoreSearchRequest;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreSearchItemResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;
import com.moodcafe.store.mapper.StoreImageMapper;
import com.moodcafe.store.mapper.StoreMapper;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.UserPreference;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

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

    @InjectMocks
    private StoreServiceImpl storeService;

    private Store store1;
    private Store store2;
    private Tag vibeTag;
    private Tag purposeTag;

    @BeforeEach
    void setUp() {
        TagCategory vibeCat = TagCategory.builder().code("VIBE").name("Phong cách").build();
        TagCategory purposeCat = TagCategory.builder().code("PURPOSE").name("Mục đích").build();

        vibeTag = Tag.builder().tagId(UUID.randomUUID()).name("Tối giản").category(vibeCat).build();
        purposeTag = Tag.builder().tagId(UUID.randomUUID()).name("Chạy deadline").category(purposeCat).build();

        store1 = Store.builder()
                .storeId(UUID.randomUUID())
                .name("The Shelter Coffee")
                .address("42 Nguyen Hue, District 1, TP.HCM")
                .priceRange("45k - 80k")
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(23, 0))
                .status(StoreStatus.ACTIVE)
                .latitude(new BigDecimal("10.7745"))
                .longitude(new BigDecimal("106.7032"))
                .build();

        store2 = Store.builder()
                .storeId(UUID.randomUUID())
                .name("Green Garden Cafe")
                .address("12 Xuan Thuy, Thao Dien, Thu Duc")
                .priceRange("30k - 50k")
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(21, 0))
                .status(StoreStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("searchStores - returns empty page when no active stores")
    void searchStores_NoStores_ReturnsEmptyPage() {
        when(storeRepository.findAllByStatus(StoreStatus.ACTIVE)).thenReturn(List.of());

        StoreSearchRequest request = StoreSearchRequest.builder().build();
        PageResponse<StoreSearchItemResponse> response = storeService.searchStores(request);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("searchStores - filters by keyword and district")
    void searchStores_FilterKeywordAndDistrict_Success() {
        when(storeRepository.findAllByStatus(StoreStatus.ACTIVE)).thenReturn(List.of(store1, store2));
        when(storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.APPROVED)).thenReturn(List.of());
        when(favoriteStoreRepository.countFavoritesGroupedByStore()).thenReturn(List.of());
        when(storeReviewRepository.findOverallRatingAndCountGroupedByStore()).thenReturn(List.of());
        when(configurationService.getMatchScoreWeights()).thenReturn(MatchScoreWeights.builder().build());

        // Search for "Shelter" in "District 1"
        StoreSearchRequest request = StoreSearchRequest.builder()
                .keyword("Shelter")
                .district("District 1")
                .build();

        PageResponse<StoreSearchItemResponse> response = storeService.searchStores(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getName()).isEqualTo("The Shelter Coffee");
        assertThat(response.getItems().get(0).getDistrict()).isEqualTo("Quận 1");
    }

    @Test
    @DisplayName("searchStores - calculates match score when user preferences exist")
    void searchStores_WithUserPreferences_CalculatesMatchScore() {
        User user = User.builder().userId(UUID.randomUUID()).build();
        when(currentUserService.getCurrentUser()).thenReturn(user);

        UserPreference pref = UserPreference.builder()
                .tag(vibeTag)
                .build();
        when(userPreferenceRepository.findAllByUserId(user.getUserId())).thenReturn(List.of(pref));

        StoreTag storeTag = StoreTag.builder()
                .storeId(store1.getStoreId())
                .tag(vibeTag)
                .status(StoreTagStatus.APPROVED)
                .build();
        when(storeTagRepository.findAllByStatusOrderByCreatedAtDesc(StoreTagStatus.APPROVED)).thenReturn(List.of(storeTag));
        when(storeRepository.findAllByStatus(StoreStatus.ACTIVE)).thenReturn(List.of(store1));
        when(favoriteStoreRepository.countFavoritesGroupedByStore()).thenReturn(Collections.singletonList(
                new Object[]{store1.getStoreId(), 100L}
        ));
        when(storeReviewRepository.findOverallRatingAndCountGroupedByStore()).thenReturn(Collections.singletonList(
                new Object[]{store1.getStoreId(), 4.8, 20L}
        ));
        when(configurationService.getMatchScoreWeights()).thenReturn(MatchScoreWeights.builder().build());

        StoreSearchRequest request = StoreSearchRequest.builder().build();
        PageResponse<StoreSearchItemResponse> response = storeService.searchStores(request);

        assertThat(response.getItems()).hasSize(1);
        StoreSearchItemResponse item = response.getItems().get(0);
        assertThat(item.getMatchScore()).isNotNull();
        assertThat(item.getMatchScore()).isGreaterThanOrEqualTo(70);
        assertThat(item.getFavoriteCount()).isEqualTo(100L);
        assertThat(item.getOverallRating()).isEqualTo(4.8);
    }

    @Test
    @DisplayName("getStoreById - returns store detail with reviews, summary, and tag scores")
    void getStoreById_ReturnsDetailWithReviewsAndTagScores() {
        UUID storeId = store1.getStoreId();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store1));
        when(storeMapper.toResponse(store1)).thenReturn(StoreResponse.builder().storeId(storeId).name(store1.getName()).build());
        when(storeImageRepository.findAllByStoreStoreId(storeId)).thenReturn(List.of());

        StoreTag approvedTag = StoreTag.builder()
                .storeId(storeId)
                .tag(vibeTag)
                .status(StoreTagStatus.APPROVED)
                .avgScore(4.5)
                .reviewCount(10)
                .build();
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of(approvedTag));
        when(storeTagMapper.toResponse(approvedTag)).thenReturn(StoreTagResponse.builder()
                .tagId(vibeTag.getTagId())
                .tagName(vibeTag.getName())
                .averageScore(4.5)
                .reviewCount(10)
                .build());

        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(Collections.singletonList(
                new Object[]{vibeTag.getTagId(), 4.8, 12L}
        ));

        StoreReview mockReview = StoreReview.builder().reviewId(UUID.randomUUID()).store(store1).build();
        when(storeReviewRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId)).thenReturn(List.of(mockReview));
        when(storeReviewMapper.toResponse(mockReview)).thenReturn(StoreReviewResponse.builder()
                .reviewId(mockReview.getReviewId())
                .storeId(storeId)
                .build());

        when(storeReviewRepository.getReviewSummaryByStoreId(storeId)).thenReturn(Collections.singletonList(
                new Object[]{4.8, 5.0, 4.0, 4.0, 5.0, 1L}
        ));

        StoreResponse response = storeService.getStoreById(storeId);

        assertThat(response).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getTags()).hasSize(1);
        assertThat(response.getTags().get(0).getAverageScore()).isEqualTo(4.8);
        assertThat(response.getTags().get(0).getReviewCount()).isEqualTo(12);
        assertThat(response.getReviews()).hasSize(1);
        assertThat(response.getReviewSummary()).isNotNull();
        assertThat(response.getReviewSummary().getAverageRating()).isEqualTo(4.8);
        assertThat(response.getOverallRating()).isEqualTo(4.8);
        assertThat(response.getReviewCount()).isEqualTo(1L);
        assertThat(response.getHasReviewed()).isFalse();
        assertThat(response.getUserReview()).isNull();
    }

    @Test
    @DisplayName("getStoreById - authenticated user has reviewed - returns hasReviewed true and userReview")
    void getStoreById_AuthenticatedUser_HasReviewed_ReturnsTrueAndUserReview() {
        UUID storeId = store1.getStoreId();
        UUID userId = UUID.randomUUID();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store1));
        when(storeMapper.toResponse(store1)).thenReturn(StoreResponse.builder().storeId(storeId).name(store1.getName()).build());
        when(storeImageRepository.findAllByStoreStoreId(storeId)).thenReturn(List.of());
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of());
        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(List.of());
        when(storeReviewRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId)).thenReturn(List.of());
        when(storeReviewRepository.getReviewSummaryByStoreId(storeId)).thenReturn(List.of());

        StoreReview myReview = StoreReview.builder().reviewId(UUID.randomUUID()).store(store1).build();
        StoreReviewResponse myReviewResponse = StoreReviewResponse.builder().reviewId(myReview.getReviewId()).storeId(storeId).build();

        when(currentUserService.isAuthenticated()).thenReturn(true);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, userId))
                .thenReturn(Optional.of(myReview));
        when(storeReviewMapper.toResponse(myReview)).thenReturn(myReviewResponse);

        StoreResponse response = storeService.getStoreById(storeId);

        assertThat(response).isNotNull();
        assertThat(response.getHasReviewed()).isTrue();
        assertThat(response.getUserReview()).isNotNull();
        assertThat(response.getUserReview().getReviewId()).isEqualTo(myReview.getReviewId());
        assertThat(response.getHasUserReviewed()).isTrue();
        assertThat(response.getMyReview()).isNotNull();
    }
}
