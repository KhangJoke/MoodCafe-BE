package com.moodcafe.store.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.store.abstraction.service.StoreReviewService;
import com.moodcafe.store.dto.request.ResolveReviewReportRequest;
import com.moodcafe.store.dto.response.ReviewReportResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Review Moderation", description = "Quản trị viên quản lý đánh giá toàn hệ thống & kiểm duyệt báo cáo vi phạm")
public class AdminReviewController {

    private final StoreReviewService storeReviewService;

    @Operation(summary = "Xem toàn bộ danh sách đánh giá trên hệ thống (Hỗ trợ lọc theo quán, số sao, trạng thái phản hồi, tìm kiếm)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoreReviewResponse>>> getAllReviews(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String replyStatus,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<StoreReviewResponse> reviews = storeReviewService.getAllReviewsAdmin(storeId, rating, replyStatus, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @Operation(summary = "Xem chi tiết một đánh giá")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<StoreReviewResponse>> getReviewById(@PathVariable UUID reviewId) {
        StoreReviewResponse review = storeReviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @Operation(summary = "Xóa mềm / Ẩn đánh giá vi phạm chính sách")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        storeReviewService.deleteReviewByAdmin(reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa đánh giá vi phạm thành công"));
    }

    @Operation(summary = "Xem danh sách các báo cáo vi phạm đánh giá (Lọc theo trạng thái PENDING, RESOLVED, DISMISSED)")
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<ReviewReportResponse>>> getAllReviewReports(
            @RequestParam(required = false) ReviewReportStatus status,
            @RequestParam(required = false) UUID storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewReportResponse> reports = storeReviewService.getAllReviewReportsAdmin(status, storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @Operation(summary = "Xử lý báo cáo vi phạm đánh giá (Duyệt xóa review hoặc Bác bỏ báo cáo)")
    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> resolveReviewReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReviewReportRequest request
    ) {
        ReviewReportResponse response = storeReviewService.resolveReviewReport(reportId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Xử lý báo cáo đánh giá thành công"));
    }
}
