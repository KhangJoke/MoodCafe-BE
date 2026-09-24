package com.moodcafe.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;

import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.ReviewTagRatingRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
import com.moodcafe.store.dto.response.ReviewTagResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.TagRating;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreReviewServiceImplTest {

    @Mock
    private StoreReviewRepository storeReviewRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private StoreReviewMapper storeReviewMapper;
    @Mock
    private TagRatingRepository tagRatingRepository;
    @Mock
    private StoreTagRepository storeTagRepository;
    @Mock
    private StoreStaffRepository storeStaffRepository;
    @Mock
    private ObjectMapper objectMapper;


    @InjectMocks
    private StoreReviewServiceImpl storeReviewService;

    private User currentUser;
    private Store store;
    private Tag tagQuiet;
    private StoreTag storeTagQuiet;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Test User")
                .role(Role.builder().name("USER").build())
                .build();

        storeId = UUID.randomUUID();
        store = Store.builder()
                .storeId(storeId)
                .name("The Shelter Coffee")
                .build();

        TagCategory vibeCat = TagCategory.builder().code("VIBE").name("Phong cách").build();
        tagQuiet = Tag.builder()
                .tagId(UUID.randomUUID())
                .name("Yên tĩnh")
                .category(vibeCat)
                .build();

        storeTagQuiet = StoreTag.builder()
                .storeTagId(UUID.randomUUID())
                .storeId(storeId)
                .tag(tagQuiet)
                .status(StoreTagStatus.APPROVED)
                .avgScore(0.0)
                .reviewCount(0)
                .build();
    }

    @Test
    @DisplayName("createReview - with tag ratings - saves review and recalculates store tag scores")
    void createReview_WithTagRatings_Success() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        CreateStoreReviewRequest request = CreateStoreReviewRequest.builder()
                .overallRating(BigDecimal.valueOf(4.5))
                .content("Quán rất yên tĩnh")
                .tagRatings(List.of(
                        ReviewTagRatingRequest.builder()
                                .tagId(tagQuiet.getTagId())
                                .score(5)
                                .build()
                ))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(fileStorageService.uploadImage(any(), eq("reviews"))).thenReturn(UploadImageResponse.builder().imageUrl("https://cloud.com/img.jpg").build());
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of(storeTagQuiet));

        when(storeReviewRepository.save(any(StoreReview.class))).thenAnswer(invocation -> {
            StoreReview r = invocation.getArgument(0);
            r.setReviewId(UUID.randomUUID());
            return r;
        });

        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(storeTagQuiet));
        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(Collections.singletonList(
                new Object[]{tagQuiet.getTagId(), 5.0, 1L}
        ));

        when(storeReviewMapper.toResponse(any(StoreReview.class))).thenReturn(StoreReviewResponse.builder()
                .reviewId(UUID.randomUUID())
                .storeId(storeId)
                .overallRating(BigDecimal.valueOf(4.5))
                .tagRatings(List.of(
                        ReviewTagResponse.builder().tagId(tagQuiet.getTagId()).tagName("Yên tĩnh").score(5).build()
                ))
                .build());

        StoreReviewResponse response = storeReviewService.createReview(storeId, request, image, null);

        assertThat(response).isNotNull();
        assertThat(response.getTagRatings()).hasSize(1);
        assertThat(response.getTagRatings().get(0).getScore()).isEqualTo(5);
        verify(storeTagRepository).save(storeTagQuiet);
        assertThat(storeTagQuiet.getAvgScore()).isEqualTo(5.0);
        assertThat(storeTagQuiet.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("createReview - tag not approved for store - throws AppException")
    void createReview_TagNotApprovedForStore_ThrowsAppException() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        UUID randomTagId = UUID.randomUUID();
        CreateStoreReviewRequest request = CreateStoreReviewRequest.builder()
                .overallRating(BigDecimal.valueOf(4.5))
                .tagRatings(List.of(
                        ReviewTagRatingRequest.builder()
                                .tagId(randomTagId)
                                .score(5)
                                .build()
                ))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(fileStorageService.uploadImage(any(), eq("reviews"))).thenReturn(UploadImageResponse.builder().imageUrl("https://cloud.com/img.jpg").build());
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of(storeTagQuiet));

        assertThatThrownBy(() -> storeReviewService.createReview(storeId, request, image, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not an approved tag");
    }

    @Test
    @DisplayName("createReview - invalid score - throws AppException")
    void createReview_InvalidScore_ThrowsAppException() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());
        CreateStoreReviewRequest request = CreateStoreReviewRequest.builder()
                .overallRating(BigDecimal.valueOf(4.5))
                .tagRatings(List.of(
                        ReviewTagRatingRequest.builder()
                                .tagId(tagQuiet.getTagId())
                                .score(6)
                                .build()
                ))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(fileStorageService.uploadImage(any(), eq("reviews"))).thenReturn(UploadImageResponse.builder().imageUrl("https://cloud.com/img.jpg").build());
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of(storeTagQuiet));

        assertThatThrownBy(() -> storeReviewService.createReview(storeId, request, image, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("between 1 and 5");
    }

    @Test
    @DisplayName("updateReview - author updates content and tag ratings - recalculates scores")
    void updateReview_AuthorUpdates_Success() {
        UUID reviewId = UUID.randomUUID();
        StoreReview existingReview = StoreReview.builder()
                .reviewId(reviewId)
                .store(store)
                .user(currentUser)
                .overallRating(BigDecimal.valueOf(4.0))
                .content("Old review")
                .build();

        existingReview.addTagRating(TagRating.builder()
                .review(existingReview)
                .tag(tagQuiet)
                .score(3)
                .build());

        UpdateStoreReviewRequest updateRequest = UpdateStoreReviewRequest.builder()
                .overallRating(BigDecimal.valueOf(5.0))
                .content("New review")
                .tagRatings(List.of(
                        ReviewTagRatingRequest.builder().tagId(tagQuiet.getTagId()).score(5).build()
                ))
                .build();

        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED)).thenReturn(List.of(storeTagQuiet));
        when(storeReviewRepository.save(any(StoreReview.class))).thenReturn(existingReview);
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(storeTagQuiet));
        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(Collections.singletonList(
                new Object[]{tagQuiet.getTagId(), 5.0, 1L}
        ));
        when(storeReviewMapper.toResponse(any(StoreReview.class))).thenReturn(StoreReviewResponse.builder().reviewId(reviewId).build());

        StoreReviewResponse response = storeReviewService.updateReview(reviewId, updateRequest, null);

        assertThat(response).isNotNull();
        assertThat(existingReview.getContent()).isEqualTo("New review");
        assertThat(existingReview.getOverallRating()).isEqualTo(BigDecimal.valueOf(5.0));
        verify(storeTagRepository).save(storeTagQuiet);
    }

    @Test
    @DisplayName("deleteReview - author deletes review - recalculates scores")
    void deleteReview_AuthorDeletes_Success() {
        UUID reviewId = UUID.randomUUID();
        StoreReview existingReview = StoreReview.builder()
                .reviewId(reviewId)
                .store(store)
                .user(currentUser)
                .build();

        when(storeReviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(storeTagQuiet));
        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(List.of());

        storeReviewService.deleteReview(reviewId);

        verify(storeReviewRepository).delete(existingReview);
        verify(storeTagRepository).save(storeTagQuiet);
        assertThat(storeTagQuiet.getAvgScore()).isEqualTo(0.0);
        assertThat(storeTagQuiet.getReviewCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("createReview - user already reviewed store - throws USER_ALREADY_REVIEWED")
    void createReview_UserAlreadyReviewed_ThrowsAppException() {
        CreateStoreReviewRequest request = CreateStoreReviewRequest.builder()
                .overallRating(BigDecimal.valueOf(4.5))
                .content("Review lại")
                .build();
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "dummy".getBytes());

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeReviewRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())).thenReturn(true);

        assertThatThrownBy(() -> storeReviewService.createReview(storeId, request, image, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_REVIEWED);
    }

    @Test
    @DisplayName("getMyReviewForStore - review exists - returns StoreReviewResponse")
    void getMyReviewForStore_Found_ReturnsResponse() {
        StoreReview userReview = StoreReview.builder()
                .reviewId(UUID.randomUUID())
                .store(store)
                .user(currentUser)
                .overallRating(BigDecimal.valueOf(5.0))
                .content("Quán tuyệt vời")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId()))
                .thenReturn(Optional.of(userReview));
        when(storeReviewMapper.toResponse(userReview)).thenReturn(StoreReviewResponse.builder()
                .reviewId(userReview.getReviewId())
                .storeId(storeId)
                .overallRating(BigDecimal.valueOf(5.0))
                .content("Quán tuyệt vời")
                .build());

        StoreReviewResponse response = storeReviewService.getMyReviewForStore(storeId);

        assertThat(response).isNotNull();
        assertThat(response.getReviewId()).isEqualTo(userReview.getReviewId());
        assertThat(response.getOverallRating()).isEqualTo(BigDecimal.valueOf(5.0));
    }

    @Test
    @DisplayName("getMyReviewForStore - no review - returns null")
    void getMyReviewForStore_NotFound_ReturnsNull() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId()))
                .thenReturn(Optional.empty());

        StoreReviewResponse response = storeReviewService.getMyReviewForStore(storeId);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("deleteMyReviewForStore - review exists - soft deletes and recalculates")
    void deleteMyReviewForStore_Success() {
        StoreReview userReview = StoreReview.builder()
                .reviewId(UUID.randomUUID())
                .store(store)
                .user(currentUser)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId()))
                .thenReturn(Optional.of(userReview));
        when(storeTagRepository.findAllByStoreId(storeId)).thenReturn(List.of(storeTagQuiet));
        when(tagRatingRepository.getAllTagRatingSummariesForStore(storeId)).thenReturn(List.of());

        storeReviewService.deleteMyReviewForStore(storeId);

        verify(storeReviewRepository).delete(userReview);
        verify(storeTagRepository).save(storeTagQuiet);
    }

    @Test
    @DisplayName("deleteMyReviewForStore - no review - throws REVIEW_NOT_FOUND")
    void deleteMyReviewForStore_NotFound_ThrowsAppException() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeReviewService.deleteMyReviewForStore(storeId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REVIEW_NOT_FOUND);
    }
}
