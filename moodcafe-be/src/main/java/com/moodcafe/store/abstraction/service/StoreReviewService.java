package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.MerchantReplyReviewRequest;
import com.moodcafe.store.dto.request.ReportReviewRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
import com.moodcafe.store.dto.response.MerchantReviewStatsResponse;
import com.moodcafe.store.dto.response.ReviewReportResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StoreReviewService {

    StoreReviewResponse createReview(UUID storeId, CreateStoreReviewRequest request, MultipartFile image, List<MultipartFile> additionalImages);

    StoreReviewResponse updateReview(UUID reviewId, UpdateStoreReviewRequest request, List<MultipartFile> newImages);

    Page<StoreReviewResponse> getStoreReviews(UUID storeId, Pageable pageable);

    Page<StoreReviewResponse> getStoreReviews(UUID storeId, Integer rating, String replyStatus, String search, Pageable pageable);

    Page<StoreReviewResponse> getMerchantReviews(UUID storeId, Integer rating, String replyStatus, String search, Pageable pageable);

    MerchantReviewStatsResponse getMerchantReviewStats(UUID storeId);

    StoreReviewResponse getReviewById(UUID reviewId);

    StoreReviewSummaryResponse getStoreReviewSummary(UUID storeId);

    StoreReviewResponse getMyReviewForStore(UUID storeId);

    StoreReviewResponse replyToReview(UUID storeId, UUID reviewId, MerchantReplyReviewRequest request);

    StoreReviewResponse updateReviewReply(UUID storeId, UUID reviewId, MerchantReplyReviewRequest request);

    void deleteReviewReply(UUID storeId, UUID reviewId);

    ReviewReportResponse reportReview(UUID storeId, UUID reviewId, ReportReviewRequest request);

    void deleteMyReviewForStore(UUID storeId);

    void deleteReview(UUID reviewId);

    Page<StoreReviewResponse> getAllReviewsAdmin(UUID storeId, Integer rating, String replyStatus, String search, Pageable pageable);

    Page<ReviewReportResponse> getAllReviewReportsAdmin(com.moodcafe.store.entity.enums.ReviewReportStatus status, UUID storeId,
                                                        UUID reviewId, com.moodcafe.store.entity.enums.ReviewReportReason reason,
                                                        Pageable pageable);

    ReviewReportResponse getReviewReportByIdAdmin(UUID reportId);

    ReviewReportResponse resolveReviewReport(UUID reportId, com.moodcafe.store.dto.request.ResolveReviewReportRequest request);

    void deleteReviewByAdmin(UUID reviewId);

    com.moodcafe.store.dto.response.ReviewReportStatisticsResponse getReviewReportStatistics();

    java.util.List<com.moodcafe.store.dto.response.TopReportedStoreResponse> getTopReportedStores(int limit);
}
