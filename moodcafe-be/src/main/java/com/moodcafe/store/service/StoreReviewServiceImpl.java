package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.abstraction.service.StoreReviewService;
import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.mapper.StoreReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreReviewServiceImpl implements StoreReviewService {

    private final StoreReviewRepository storeReviewRepository;
    private final StoreRepository storeRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final StoreReviewMapper storeReviewMapper;

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

        review = storeReviewRepository.save(review);
        log.info("User {} submitted review {} for store {}", currentUser.getUserId(), review.getReviewId(), storeId);

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

        storeReviewRepository.delete(review);
        log.info("Review {} soft deleted by user {}", reviewId, currentUser.getUserId());
    }

    private Double roundToOneDecimal(Double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
