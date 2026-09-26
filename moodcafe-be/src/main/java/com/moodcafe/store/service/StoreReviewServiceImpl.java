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
import com.moodcafe.store.abstraction.repository.ReviewReportRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.MerchantReplyReviewRequest;
import com.moodcafe.store.dto.request.ReportReviewRequest;
import com.moodcafe.store.dto.request.ReviewTagRatingRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
import com.moodcafe.store.dto.response.MerchantReviewStatsResponse;
import com.moodcafe.store.dto.response.ReviewReportResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.ReviewReport;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.TagRating;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
import com.moodcafe.store.mapper.StoreReviewMapper;
import com.moodcafe.tag.abstraction.repository.StoreTagRepository;
import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.repository.VisitVerificationRepository;
import com.moodcafe.store.entity.VisitVerification;
import com.moodcafe.store.entity.enums.VisitVerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
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
    private final StoreStaffRepository storeStaffRepository;
    private final VisitVerificationRepository visitVerificationRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final StoreReviewMapper storeReviewMapper;
    private final TagRatingRepository tagRatingRepository;
    private final StoreTagRepository storeTagRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final StoreStaffService storeStaffService;
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

        // 1. Business Rule: Owner or Staff cannot review their own store
        if (storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_STAFF_ACTION,
                    "Chủ quán hoặc nhân viên không được phép đánh giá quán của mình");
        }

        // 2. Business Rule: Only 1 review per user per store
        if (storeReviewRepository.existsByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.USER_ALREADY_REVIEWED);
        }

        // 3. Validate VisitVerification if provided (Verified Review path)
        VisitVerification visitVerification = null;
        if (request.getVisitVerificationId() != null) {
            visitVerification = visitVerificationRepository.findById(request.getVisitVerificationId())
                    .orElseThrow(() -> new AppException(ErrorCode.VISIT_VERIFICATION_NOT_FOUND));

            if (!visitVerification.getUser().getUserId().equals(currentUser.getUserId())) {
                throw new AppException(ErrorCode.VISIT_VERIFICATION_USER_MISMATCH);
            }

            if (!visitVerification.getStore().getStoreId().equals(storeId)) {
                throw new AppException(ErrorCode.VISIT_VERIFICATION_STORE_MISMATCH);
            }

            if (visitVerification.getStatus() != VisitVerificationStatus.VERIFIED) {
                throw new AppException(ErrorCode.VISIT_VERIFICATION_INVALID);
            }

            if (visitVerification.getExpiresAt().isBefore(Instant.now())) {
                throw new AppException(ErrorCode.VISIT_VERIFICATION_EXPIRED);
            }

            if (visitVerification.isUsed()) {
                throw new AppException(ErrorCode.VISIT_VERIFICATION_ALREADY_USED);
            }
        }

        // 4. Validate image counts (Max 3 images rule)
        boolean hasPrimaryFile = image != null && !image.isEmpty();
        int additionalFilesCount = additionalImages != null
                ? (int) additionalImages.stream().filter(f -> f != null && !f.isEmpty()).count()
                : 0;
        int totalProvidedFiles = (hasPrimaryFile ? 1 : 0) + additionalFilesCount;
        int totalImagesCount = totalProvidedFiles + (!hasPrimaryFile && visitVerification != null ? 1 : 0);

        if (totalImagesCount > 3) {
            throw new AppException(ErrorCode.MAX_REVIEW_IMAGES_EXCEEDED, "Đánh giá chỉ được tải lên tối đa 3 ảnh");
        }

        if (totalImagesCount == 0) {
            throw new AppException(ErrorCode.REVIEW_IMAGE_REQUIRED, "A live photo of the store taken at the moment is required to submit a review");
        }

        // 5. Upload images to Cloudinary with orphan protection
        List<String> uploadedImageUrls = new ArrayList<>();
        List<ReviewImage> reviewImages = new ArrayList<>();

        try {
            if (hasPrimaryFile) {
                UploadImageResponse primaryUpload = fileStorageService.uploadImage(image, "reviews");
                uploadedImageUrls.add(primaryUpload.getImageUrl());
                reviewImages.add(ReviewImage.builder().imageUrl(primaryUpload.getImageUrl()).build());
            } else if (visitVerification != null) {
                // Reuse photo from verified snap
                reviewImages.add(ReviewImage.builder().imageUrl(visitVerification.getImageUrl()).build());
            }

            if (additionalImages != null && !additionalImages.isEmpty()) {
                for (MultipartFile additionalFile : additionalImages) {
                    if (additionalFile != null && !additionalFile.isEmpty()) {
                        UploadImageResponse additionalUpload = fileStorageService.uploadImage(additionalFile, "reviews");
                        uploadedImageUrls.add(additionalUpload.getImageUrl());
                        reviewImages.add(ReviewImage.builder().imageUrl(additionalUpload.getImageUrl()).build());
                    }
                }
            }
        } catch (Exception uploadEx) {
            for (String url : uploadedImageUrls) {
                try {
                    fileStorageService.deleteImageByUrl(url);
                } catch (Exception ex) {
                    log.warn("Failed to cleanup image {}: {}", url, ex.getMessage());
                }
            }
            throw uploadEx;
        }

        // 6. Build and persist review, then mark visit as used
        StoreReview review = StoreReview.builder()
                .store(store)
                .user(currentUser)
                .visitVerificationId(visitVerification != null ? visitVerification.getVisitVerificationId() : null)
                .overallRating(request.getOverallRating())
                .quietnessRating(request.getQuietnessRating())
                .lightingRating(request.getLightingRating())
                .seatingRating(request.getSeatingRating())
                .outletRating(request.getOutletRating())
                .content(request.getContent())
                .build();

        for (ReviewImage img : reviewImages) {
            review.addImage(img);
        }

        List<ReviewTagRatingRequest> resolvedTagRatings = resolveTagRatings(request.getTagRatings(), request.getTagRatingsRaw());
        if (!resolvedTagRatings.isEmpty()) {
            applyTagRatingsToReview(review, storeId, resolvedTagRatings);
        }

        try {
            review = storeReviewRepository.save(review);
            if (visitVerification != null) {
                visitVerification.setUsed(true);
                visitVerificationRepository.save(visitVerification);
            }
        } catch (Exception dbEx) {
            log.error("Failed to persist StoreReview. Cleaning up uploaded images: {}", dbEx.getMessage());
            for (String url : uploadedImageUrls) {
                try {
                    fileStorageService.deleteImageByUrl(url);
                } catch (Exception ex) {
                    log.warn("Failed to cleanup image {}: {}", url, ex.getMessage());
                }
            }
            throw dbEx;
        }

        if (!resolvedTagRatings.isEmpty()) {
            recalculateStoreTagScores(storeId);
        }

        log.info("User {} submitted review {} (verified: {}) with {} tag ratings for store {}",
                currentUser.getUserId(), review.getReviewId(), visitVerification != null, review.getTagRatings().size(), storeId);

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
        return getStoreReviews(storeId, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreReviewResponse> getStoreReviews(UUID storeId, Integer rating, String replyStatus, String search, Pageable pageable) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        Specification<StoreReview> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("store").get("storeId"), storeId));

            if (rating != null && rating >= 1 && rating <= 5) {
                if (rating == 5) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("overallRating"), new BigDecimal("4.5")));
                } else {
                    BigDecimal minR = BigDecimal.valueOf(rating);
                    BigDecimal maxR = BigDecimal.valueOf(rating + 1);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("overallRating"), minR));
                    predicates.add(cb.lessThan(root.get("overallRating"), maxR));
                }
            }

            if (replyStatus != null && !replyStatus.isBlank() && !"ALL".equalsIgnoreCase(replyStatus)) {
                if ("REPLIED".equalsIgnoreCase(replyStatus)) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("merchantReply")),
                            cb.notEqual(cb.trim(root.get("merchantReply")), "")
                    ));
                } else if ("NOT_REPLIED".equalsIgnoreCase(replyStatus)) {
                    predicates.add(cb.or(
                            cb.isNull(root.get("merchantReply")),
                            cb.equal(cb.trim(root.get("merchantReply")), "")
                    ));
                }
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate fullNameMatch = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                Predicate contentMatch = cb.like(cb.lower(root.get("content")), pattern);
                predicates.add(cb.or(fullNameMatch, contentMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return storeReviewRepository.findAll(spec, pageable).map(storeReviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreReviewResponse> getMerchantReviews(UUID storeId, Integer rating, String replyStatus, String search, Pageable pageable) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");
        return getStoreReviews(storeId, rating, replyStatus, search, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantReviewStatsResponse getMerchantReviewStats(UUID storeId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        long totalReviews = storeReviewRepository.countByStoreStoreId(storeId);
        long repliedCount = storeReviewRepository.countRepliedByStoreId(storeId);
        long unrepliedCount = storeReviewRepository.countUnrepliedByStoreId(storeId);

        List<Object[]> summary = storeReviewRepository.getReviewSummaryByStoreId(storeId);
        double averageRating = 0.0;
        if (summary != null && !summary.isEmpty() && summary.get(0)[0] != null) {
            averageRating = roundToOneDecimal(((Number) summary.get(0)[0]).doubleValue());
        }

        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            starCounts.put(i, 0L);
        }

        List<Object[]> ratingGroups = storeReviewRepository.countByRatingGroupedByStoreId(storeId);
        if (ratingGroups != null) {
            for (Object[] row : ratingGroups) {
                if (row[0] != null && row[1] != null) {
                    int star = ((Number) row[0]).intValue();
                    long count = ((Number) row[1]).longValue();
                    if (star >= 1 && star <= 5) {
                        starCounts.put(star, count);
                    }
                }
            }
        }

        return MerchantReviewStatsResponse.builder()
                .storeId(storeId)
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .repliedCount(repliedCount)
                .unrepliedCount(unrepliedCount)
                .starCounts(starCounts)
                .build();
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
    public StoreReviewResponse replyToReview(UUID storeId, UUID reviewId, MerchantReplyReviewRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đánh giá không thuộc về quán này");
        }

        review.setMerchantReply(request.getReply());
        review.setReplyAt(Instant.now());
        review = storeReviewRepository.save(review);

        return storeReviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public StoreReviewResponse updateReviewReply(UUID storeId, UUID reviewId, MerchantReplyReviewRequest request) {
        return replyToReview(storeId, reviewId, request);
    }

    @Override
    @Transactional
    public void deleteReviewReply(UUID storeId, UUID reviewId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đánh giá không thuộc về quán này");
        }

        review.setMerchantReply(null);
        review.setReplyAt(null);
        storeReviewRepository.save(review);
    }

    @Override
    @Transactional
    public ReviewReportResponse reportReview(UUID storeId, UUID reviewId, ReportReviewRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        StoreReview review = storeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Đánh giá không thuộc về quán này");
        }

        if (reviewReportRepository.existsByReviewReviewIdAndReporterUserId(reviewId, currentUser.getUserId())) {
            throw new AppException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        ReviewReport report = ReviewReport.builder()
                .review(review)
                .reporter(currentUser)
                .store(review.getStore())
                .reason(request.getReason())
                .details(request.getDetails())
                .status(ReviewReportStatus.PENDING)
                .build();

        report = reviewReportRepository.save(report);

        return ReviewReportResponse.builder()
                .reportId(report.getReportId())
                .reviewId(review.getReviewId())
                .reporterUserId(currentUser.getUserId())
                .reporterFullName(currentUser.getFullName())
                .storeId(storeId)
                .storeName(review.getStore().getName())
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
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
