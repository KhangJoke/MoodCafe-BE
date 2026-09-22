package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.StoreReviewService;
import com.moodcafe.store.dto.request.CreateStoreReviewRequest;
import com.moodcafe.store.dto.request.UpdateStoreReviewRequest;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.dto.response.StoreReviewSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
@Tag(name = "Store Reviews", description = "Quản lý đánh giá quán cà phê và Verified Reviews")
public class StoreReviewController {

    private final StoreReviewService storeReviewService;

    @Operation(summary = "Gửi đánh giá quán (Hỗ trợ Verified Review với visitVerificationId, tối đa 3 ảnh)",
            description = "Nếu có visitVerificationId từ bước Snap/Survey, đánh giá sẽ được đánh dấu Verified. Có thể tái sử dụng ảnh Snap nếu không tải lên ảnh mới.")
    @PostMapping(value = "/{storeId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoreReviewResponse>> createReview(
            @PathVariable UUID storeId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @Valid @ModelAttribute CreateStoreReviewRequest request
    ) {
        StoreReviewResponse response = storeReviewService.createReview(storeId, request, image, additionalImages);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Review submitted successfully"));
    }

    @GetMapping("/{storeId}/reviews")
    public ResponseEntity<ApiResponse<Page<StoreReviewResponse>>> getStoreReviews(
            @PathVariable UUID storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<StoreReviewResponse> reviews = storeReviewService.getStoreReviews(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/{storeId}/reviews/me")
    public ResponseEntity<ApiResponse<StoreReviewResponse>> getMyReviewForStore(
            @PathVariable UUID storeId
    ) {
        StoreReviewResponse response = storeReviewService.getMyReviewForStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{storeId}/reviews/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyReviewForStore(
            @PathVariable UUID storeId
    ) {
        storeReviewService.deleteMyReviewForStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review removed successfully"));
    }

    @GetMapping("/{storeId}/reviews/summary")
    public ResponseEntity<ApiResponse<StoreReviewSummaryResponse>> getStoreReviewSummary(
            @PathVariable UUID storeId
    ) {
        StoreReviewSummaryResponse summary = storeReviewService.getStoreReviewSummary(storeId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<StoreReviewResponse>> getReviewById(
            @PathVariable UUID reviewId
    ) {
        StoreReviewResponse review = storeReviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoreReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @RequestParam(value = "newImages", required = false) List<MultipartFile> newImages,
            @Valid @ModelAttribute UpdateStoreReviewRequest request
    ) {
        StoreReviewResponse response = storeReviewService.updateReview(reviewId, request, newImages);
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }

    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<StoreReviewResponse>> updateReviewJson(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateStoreReviewRequest request
    ) {
        StoreReviewResponse response = storeReviewService.updateReview(reviewId, request, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Review updated successfully"));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId
    ) {
        storeReviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }
}
