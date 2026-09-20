package com.moodcafe.store.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.repository.TagRatingRepository;
import com.moodcafe.store.abstraction.service.StoreReviewService;
import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.ReviewTagRatingRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.TagRating;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreReviewServiceImpl implements StoreReviewService {

    private final StoreReviewRepository storeReviewRepository;
    private final StoreRepository storeRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final StoreReviewMapper storeReviewMapper;
    private final TagRatingRepository tagRatingRepository;
    private final StoreTagRepository storeTagRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StoreReviewResponse createReview(
            UUID storeId,
            CreateStoreReviewRequest request,
            MultipartFile image,
            List<MultipartFile> additionalImages
    ) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (storeReviewRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.USER_ALREADY_REVIEWED);
        }

        // Validation: A photo taken at the moment is strictly required
        if (image == null || image.isEmpty()) {
            throw new AppException(ErrorCode.REVIEW_IMAGE_REQUIRED);
        }

        // Upload live moment photo to Cloudinary
        UploadImageResponse primaryUpload = fileStorageService.uploadImage(image, "reviews");

        StoreReview review = StoreReview.builder()
                .store(store)
                .user(currentUser)
                .visitVerificationId(request.getVisitVerificationId())
                .overallRating(request.getOverallRating())
                .quietnessRating(request.getQuietnessRating())
                .lightingRating(request.getLightingRating())
                .seatingRating(request.getSeatingRating())
                .outletRating(request.getOutletRating())
                .content(request.getContent())
                .build();

        ReviewImage primaryImage = ReviewImage.builder()
                .review(review)
                .imageUrl(primaryUpload.getImageUrl())
                .build();
        review.addImage(primaryImage);

        // Upload any extra images if provided
        if (additionalImages != null && !additionalImages.isEmpty()) {
            for (MultipartFile additionalFile : additionalImages) {
                if (additionalFile != null && !additionalFile.isEmpty()) {
                    UploadImageResponse additionalUpload = fileStorageService.uploadImage(additionalFile, "reviews");
                    ReviewImage extraImage = ReviewImage.builder()
                            .review(review)
                            .imageUrl(additionalUpload.getImageUrl())
                            .build();
                    review.addImage(extraImage);
                }
            }
        }

        // Attach tag ratings if provided
        List<ReviewTagRatingRequest> resolvedTagRatings = resolveTagRatings(request.getTagRatings(), request.getTagRatingsRaw());
        if (!resolvedTagRatings.isEmpty()) {
            applyTagRatingsToReview(review, storeId, resolvedTagRatings);
        }

        review = storeReviewRepository.save(review);
        if (!resolvedTagRatings.isEmpty()) {
            recalculateStoreTagScores(storeId);
        }

        log.info("User {} submitted review {} with {} tag ratings for store {}",
                currentUser.getUserId(), review.getReviewId(), review.getTagRatings().size(), storeId);

        return storeReviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public StoreReviewResponse updateReview(UUID reviewId, UpdateStoreReviewRequest request, List<MultipartFile> newImages) {
        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User currentUser = currentUserService.getCurrentUser();
        boolean isAuthor = review.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (!isAuthor && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN_REVIEW_ACTION);
        }

        UUID storeId = review.getStore().getStoreId();

        if (request != null) {
            if (request.getOverallRating() != null) review.setOverallRating(request.getOverallRating());
            if (request.getQuietnessRating() != null) review.setQuietnessRating(request.getQuietnessRating());
            if (request.getLightingRating() != null) review.setLightingRating(request.getLightingRating());
            if (request.getSeatingRating() != null) review.setSeatingRating(request.getSeatingRating());
            if (request.getOutletRating() != null) review.setOutletRating(request.getOutletRating());
            if (request.getContent() != null) review.setContent(request.getContent());

            List<ReviewTagRatingRequest> resolvedTagRatings = resolveTagRatings(request.getTagRatings(), request.getTagRatingsRaw());
            if (resolvedTagRatings != null && !resolvedTagRatings.isEmpty()) {
                review.getTagRatings().clear();
                applyTagRatingsToReview(review, storeId, resolvedTagRatings);
            }
        }

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile newImage : newImages) {
                if (newImage != null && !newImage.isEmpty()) {
                    UploadImageResponse additionalUpload = fileStorageService.uploadImage(newImage, "reviews");
                    ReviewImage extraImage = ReviewImage.builder()
                            .review(review)
                            .imageUrl(additionalUpload.getImageUrl())
                            .build();
                    review.addImage(extraImage);
                }
            }
        }

        review = storeReviewRepository.save(review);
        recalculateStoreTagScores(storeId);
        log.info("Review {} updated by user {}", reviewId, currentUser.getUserId());

        return storeReviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreReviewResponse> getStoreReviews(UUID storeId, Pageable pageable) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return storeReviewRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId, pageable)
                .map(storeReviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreReviewResponse getReviewById(UUID reviewId) {
        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return storeReviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreReviewSummaryResponse getStoreReviewSummary(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        List<Object[]> summary = storeReviewRepository.getReviewSummaryByStoreId(storeId);
        if (summary == null || summary.isEmpty() || summary.get(0)[5] == null || ((Number) summary.get(0)[5]).longValue() == 0) {
            return StoreReviewSummaryResponse.builder()
                    .storeId(storeId)
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .averageQuietness(0.0)
                    .averageLighting(0.0)
                    .averageSeating(0.0)
                    .averageOutlet(0.0)
                    .build();
        }

        Object[] row = summary.get(0);
        return StoreReviewSummaryResponse.builder()
                .storeId(storeId)
                .averageRating(row[0] != null ? roundToOneDecimal(((Number) row[0]).doubleValue()) : 0.0)
                .averageQuietness(row[1] != null ? roundToOneDecimal(((Number) row[1]).doubleValue()) : null)
                .averageLighting(row[2] != null ? roundToOneDecimal(((Number) row[2]).doubleValue()) : null)
                .averageSeating(row[3] != null ? roundToOneDecimal(((Number) row[3]).doubleValue()) : null)
                .averageOutlet(row[4] != null ? roundToOneDecimal(((Number) row[4]).doubleValue()) : null)
                .totalReviews(((Number) row[5]).longValue())
                .build();
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {
        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        User currentUser = currentUserService.getCurrentUser();
        boolean isAuthor = review.getUser().getUserId().equals(currentUser.getUserId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (!isAuthor && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN_REVIEW_ACTION);
        }

        UUID storeId = review.getStore().getStoreId();
        storeReviewRepository.delete(review);
        recalculateStoreTagScores(storeId);
        log.info("Review {} soft deleted by user {}", reviewId, currentUser.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public StoreReviewResponse getMyReviewForStore(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();
        return storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId())
                .map(storeReviewMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteMyReviewForStore(UUID storeId) {
        User currentUser = currentUserService.getCurrentUser();
        StoreReview review = storeReviewRepository.findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(storeId, currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        storeReviewRepository.delete(review);
        recalculateStoreTagScores(storeId);
        log.info("User {} deleted review {} for store {}", currentUser.getUserId(), review.getReviewId(), storeId);
    }

    private void applyTagRatingsToReview(StoreReview review, UUID storeId, List<ReviewTagRatingRequest> ratingRequests) {
        List<StoreTag> approvedStoreTags = storeTagRepository.findAllByStoreIdAndStatus(storeId, StoreTagStatus.APPROVED);
        Map<UUID, Tag> approvedTagMap = approvedStoreTags.stream()
                .filter(st -> st.getTag() != null)
                .collect(Collectors.toMap(st -> st.getTag().getTagId(), StoreTag::getTag, (a, b) -> a));
        Map<UUID, Tag> storeTagIdMap = approvedStoreTags.stream()
                .filter(st -> st.getTag() != null)
                .collect(Collectors.toMap(StoreTag::getStoreTagId, StoreTag::getTag, (a, b) -> a));

        Set<UUID> seenTagIds = new HashSet<>();
        for (ReviewTagRatingRequest req : ratingRequests) {
            if (req == null) continue;

            Tag tag = null;
            if (req.getTagId() != null) {
                tag = approvedTagMap.get(req.getTagId());
            }
            if (tag == null && req.getStoreTagId() != null) {
                tag = storeTagIdMap.get(req.getStoreTagId());
            }

            if (tag == null) {
                throw new AppException(ErrorCode.INVALID_INPUT,
                        "Tag " + (req.getTagId() != null ? req.getTagId() : req.getStoreTagId()) + " is not an approved tag for this store");
            }

            if (req.getScore() == null || req.getScore() < 1 || req.getScore() > 5) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Tag score must be between 1 and 5");
            }

            if (seenTagIds.add(tag.getTagId())) {
                TagRating tr = TagRating.builder()
                        .review(review)
                        .tag(tag)
                        .score(req.getScore())
                        .build();
                review.addTagRating(tr);
            }
        }
    }

    private void recalculateStoreTagScores(UUID storeId) {
        List<StoreTag> storeTags = storeTagRepository.findAllByStoreId(storeId);
        if (storeTags.isEmpty()) return;

        List<Object[]> summaries = tagRatingRepository.getAllTagRatingSummariesForStore(storeId);
        Map<UUID, Object[]> scoreMap = new HashMap<>();
        for (Object[] row : summaries) {
            if (row != null && row.length >= 3 && row[0] != null) {
                scoreMap.put((UUID) row[0], row);
            }
        }

        for (StoreTag st : storeTags) {
            if (st.getTag() == null) continue;
            Object[] stats = scoreMap.get(st.getTag().getTagId());
            if (stats != null && stats[1] != null && stats[2] != null) {
                double avg = roundToOneDecimal(((Number) stats[1]).doubleValue());
                int count = ((Number) stats[2]).intValue();
                st.setAvgScore(avg);
                st.setReviewCount(count);
            } else {
                st.setAvgScore(0.0);
                st.setReviewCount(0);
            }
            storeTagRepository.save(st);
        }
    }

    private List<ReviewTagRatingRequest> resolveTagRatings(List<ReviewTagRatingRequest> list, String rawJson) {
        if (list != null && !list.isEmpty()) {
            return list;
        }
        if (rawJson != null && !rawJson.isBlank()) {
            try {
                return objectMapper.readValue(rawJson, new TypeReference<List<ReviewTagRatingRequest>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse tagRatingsRaw JSON: {}", e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private Double roundToOneDecimal(Double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
