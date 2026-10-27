package com.moodcafe.gamification.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.gamification.abstraction.repository.UserTitleRepository;
import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import com.moodcafe.gamification.abstraction.service.UserPublicProfileService;
import com.moodcafe.gamification.dto.response.EquippedTitleDto;
import com.moodcafe.gamification.dto.response.UserPublicProfileResponse;
import com.moodcafe.gamification.dto.response.UserPublicReviewResponse;
import com.moodcafe.gamification.dto.response.UserVibeSnapResponse;
import com.moodcafe.gamification.entity.UserTitle;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.VisitVerification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPublicProfileServiceImpl implements UserPublicProfileService {

    private final UserRepository userRepository;
    private final UserTitleRepository userTitleRepository;
    private final CustomerTitleService customerTitleService;
    private final StoreReviewRepository storeReviewRepository;
    private final VisitVerificationRepository visitVerificationRepository;
    private final StoreImageRepository storeImageRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với id: " + userId));

        EquippedTitleDto equippedTitle = customerTitleService.getEquippedTitle(userId);

        List<UserTitle> unlockedUserTitles = userTitleRepository
                .findAllByUserUserIdAndIsDeletedFalseOrderByUnlockedAtDesc(userId);

        List<EquippedTitleDto> unlockedTitles = unlockedUserTitles.stream()
                .map(ut -> EquippedTitleDto.builder()
                        .titleId(ut.getTitle().getTitleId())
                        .code(ut.getTitle().getCode())
                        .name(ut.getTitle().getName())
                        .tier(ut.getTitle().getTier().name())
                        .colorHex(ut.getTitle().getColorHex())
                        .iconUrl(ut.getTitle().getIconUrl())
                        .build())
                .toList();

        List<StoreReview> reviews = storeReviewRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId);
        List<VisitVerification> vibeSnaps = visitVerificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId);

        String joinedDisplay = user.getCreatedAt() != null ? DATE_FORMATTER.format(user.getCreatedAt()) : "";

        return UserPublicProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .joinedAt(user.getCreatedAt())
                .joinedDisplay(joinedDisplay)
                .equippedTitle(equippedTitle)
                .unlockedTitles(unlockedTitles)
                .stats(UserPublicProfileResponse.UserStatsDto.builder()
                        .totalReviews(reviews.size())
                        .totalVibeSnaps(vibeSnaps.size())
                        .totalTitlesUnlocked(unlockedTitles.size())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserPublicReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với id: " + userId);
        }

        Page<StoreReview> page = storeReviewRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UserPublicReviewResponse> items = page.getContent().stream()
                .map(review -> {
                    String storeImageUrl = null;
                    if (review.getStore() != null) {
                        List<StoreImage> images = storeImageRepository.findAllByStoreStoreId(review.getStore().getStoreId());
                        storeImageUrl = images.stream()
                                .filter(StoreImage::isPrimary)
                                .map(StoreImage::getImageUrl)
                                .findFirst()
                                .orElseGet(() -> images.stream().map(StoreImage::getImageUrl).findFirst().orElse(null));
                    }

                    List<String> reviewImages = review.getImages() != null
                            ? review.getImages().stream().map(ReviewImage::getImageUrl).toList()
                            : List.of();

                    boolean verified = review.getVisitVerificationId() != null || review.getBookingId() != null;

                    return UserPublicReviewResponse.builder()
                            .reviewId(review.getReviewId())
                            .storeId(review.getStore() != null ? review.getStore().getStoreId() : null)
                            .storeName(review.getStore() != null ? review.getStore().getName() : null)
                            .storeAddress(review.getStore() != null ? review.getStore().getAddress() : null)
                            .storeImageUrl(storeImageUrl)
                            .overallRating(review.getOverallRating())
                            .content(review.getContent())
                            .imageUrls(reviewImages)
                            .verified(verified)
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .toList();

        return PageResponse.<UserPublicReviewResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserVibeSnapResponse> getUserVibeSnaps(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với id: " + userId);
        }

        Page<VisitVerification> page = visitVerificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable);

        List<UserVibeSnapResponse> items = page.getContent().stream()
                .map(vv -> UserVibeSnapResponse.builder()
                        .visitVerificationId(vv.getVisitVerificationId())
                        .storeId(vv.getStore() != null ? vv.getStore().getStoreId() : null)
                        .storeName(vv.getStore() != null ? vv.getStore().getName() : null)
                        .imageUrl(vv.getImageUrl())
                        .status(vv.getStatus() != null ? vv.getStatus().name() : null)
                        .capturedAt(vv.getCapturedAt())
                        .build())
                .toList();

        return PageResponse.<UserVibeSnapResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
