package com.moodcafe.sponsored.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.sponsored.abstraction.service.SponsoredListingService;
import com.moodcafe.sponsored.dto.request.CreateSponsoredPlanRequest;
import com.moodcafe.sponsored.dto.request.UpdateSponsoredPlanRequest;
import com.moodcafe.sponsored.dto.response.SponsoredAdminStatsResponse;
import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPlanResponse;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sponsored-plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Sponsored Management", description = "Quản trị viên quản lý các gói vị trí tài trợ (CRUD, Bật/Tắt, Tra cứu chiến dịch các quán)")
public class AdminSponsoredPlanController {

    private final SponsoredListingService sponsoredListingService;

    @Operation(summary = "Xem toàn bộ danh sách gói tài trợ (Bao gồm cả gói đang tắt active)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SponsoredPlanResponse>>> getAllPlans() {
        List<SponsoredPlanResponse> plans = sponsoredListingService.getAllPlansAdmin();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @Operation(summary = "Xem chi tiết một gói tài trợ")
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SponsoredPlanResponse>> getPlanById(@PathVariable UUID planId) {
        SponsoredPlanResponse plan = sponsoredListingService.getPlanByIdAdmin(planId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @Operation(summary = "Tạo mới một gói vị trí tài trợ")
    @PostMapping
    public ResponseEntity<ApiResponse<SponsoredPlanResponse>> createPlan(
            @Valid @RequestBody CreateSponsoredPlanRequest request
    ) {
        SponsoredPlanResponse plan = sponsoredListingService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(plan, "Tạo gói vị trí tài trợ thành công"));
    }

    @Operation(summary = "Cập nhật thông tin gói tài trợ (Giá, Vị trí, Số ngày, Mô tả, Badge...)")
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<SponsoredPlanResponse>> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdateSponsoredPlanRequest request
    ) {
        SponsoredPlanResponse plan = sponsoredListingService.updatePlan(planId, request);
        return ResponseEntity.ok(ApiResponse.success(plan, "Cập nhật gói tài trợ thành công"));
    }

    @Operation(summary = "Bật / Tắt trạng thái hoạt động của gói tài trợ (Toggle Active Status)")
    @PatchMapping("/{planId}/toggle-status")
    public ResponseEntity<ApiResponse<SponsoredPlanResponse>> togglePlanStatus(@PathVariable UUID planId) {
        SponsoredPlanResponse plan = sponsoredListingService.togglePlanStatus(planId);
        String msg = plan.isActive() ? "Đã kích hoạt gói tài trợ thành công" : "Đã tạm dừng gói tài trợ thành công";
        return ResponseEntity.ok(ApiResponse.success(plan, msg));
    }

    @Operation(summary = "Xóa mềm gói tài trợ")
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID planId) {
        sponsoredListingService.deletePlan(planId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa gói tài trợ thành công"));
    }

    @Operation(summary = "Xem toàn bộ danh sách chiến dịch tài trợ của các quán trên hệ thống (Hỗ trợ tìm kiếm & lọc)")
    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<PageResponse<SponsoredListingResponse>>> getAllCampaigns(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SponsoredPlacement placement,
            @RequestParam(required = false) SponsoredListingStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SponsoredListingResponse> campaigns = sponsoredListingService.getAllCampaignsAdmin(
                search, placement, status, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @Operation(summary = "Thống kê tổng quan tài trợ quảng bá (Tổng doanh thu, Doanh thu tháng, Chiến dịch active theo vị trí)")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SponsoredAdminStatsResponse>> getSponsoredStatistics() {
        SponsoredAdminStatsResponse stats = sponsoredListingService.getSponsoredStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
