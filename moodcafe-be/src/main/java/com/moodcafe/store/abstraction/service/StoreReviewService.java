package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
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

    StoreReviewResponse getReviewById(UUID reviewId);

    StoreReviewSummaryResponse getStoreReviewSummary(UUID storeId);

    void deleteReview(UUID reviewId);
}
