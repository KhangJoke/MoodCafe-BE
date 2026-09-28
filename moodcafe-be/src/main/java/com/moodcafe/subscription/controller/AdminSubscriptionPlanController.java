package com.moodcafe.subscription.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.moodcafe.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.moodcafe.subscription.dto.response.SubscriptionPaymentResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPlanResponse;
import com.moodcafe.subscription.dto.response.UserSubscriptionResponse;
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
@RequestMapping("/api/admin/subscription-plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Subscription Management", description = "Quản trị viên quản lý các gói dịch vụ (CRUD, Bật/Tắt active, Tra cứu giao dịch đăng ký)")
public class AdminSubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Xem toàn bộ danh sách gói dịch vụ (Bao gồm cả gói đang tắt active)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAllPlans() {
        List<SubscriptionPlanResponse> plans = subscriptionService.getAllPlansAdmin();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @Operation(summary = "Xem chi tiết một gói dịch vụ")
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanById(@PathVariable UUID planId) {
        SubscriptionPlanResponse plan = subscriptionService.getPlanByIdAdmin(planId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @Operation(summary = "Tạo mới một gói dịch vụ")
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request
    ) {
        SubscriptionPlanResponse plan = subscriptionService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(plan, "Tạo gói dịch vụ thành công"));
    }

    @Operation(summary = "Cập nhật thông tin gói dịch vụ (Giá, Quyền lợi, Hạn mức chi nhánh...)")
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        SubscriptionPlanResponse plan = subscriptionService.updatePlan(planId, request);
        return ResponseEntity.ok(ApiResponse.success(plan, "Cập nhật gói dịch vụ thành công"));
    }

    @Operation(summary = "Bật / Tắt trạng thái hoạt động của gói dịch vụ (Toggle Active Status)")
    @PatchMapping("/{planId}/toggle-status")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> togglePlanStatus(@PathVariable UUID planId) {
        SubscriptionPlanResponse plan = subscriptionService.togglePlanStatus(planId);
        String msg = plan.isActive() ? "Đã kích hoạt gói dịch vụ thành công" : "Đã tạm dừng gói dịch vụ thành công";
        return ResponseEntity.ok(ApiResponse.success(plan, msg));
    }

    @Operation(summary = "Xóa mềm gói dịch vụ")
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID planId) {
        subscriptionService.deletePlan(planId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa gói dịch vụ thành công"));
    }

    @Operation(summary = "Xem toàn bộ lịch sử giao dịch mua gói trên toàn hệ thống (Hỗ trợ tìm kiếm & lọc)")
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionPaymentResponse>>> getAllPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus status,
            @RequestParam(required = false) UUID planId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SubscriptionPaymentResponse> payments = subscriptionService.getAllPaymentsAdmin(search, status, planId, pageable);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @Operation(summary = "Xem danh sách đăng ký gói của tất cả chủ quán (Tìm kiếm tên/email/phone, lọc theo gói & trạng thái)")
    @GetMapping("/merchants")
    public ResponseEntity<ApiResponse<PageResponse<UserSubscriptionResponse>>> getAllUserSubscriptions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) com.moodcafe.subscription.entity.enums.SubscriptionStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserSubscriptionResponse> subs = subscriptionService.getAllUserSubscriptionsAdmin(search, planId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(subs));
    }

    @Operation(summary = "Xem lịch sử đăng ký & thanh toán của một chủ quán cụ thể")
    @GetMapping("/merchants/{ownerUserId}/payments")
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionPaymentResponse>>> getOwnerPaymentHistory(
            @PathVariable UUID ownerUserId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SubscriptionPaymentResponse> history = subscriptionService.getOwnerPaymentHistoryAdmin(ownerUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "Thống kê tổng quan Subscription (Tổng chủ quán, Phân bố gói, Trạng thái, Doanh thu, Mới vs Gia hạn)")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<com.moodcafe.subscription.dto.response.SubscriptionStatisticsResponse>> getSubscriptionStatistics() {
        com.moodcafe.subscription.dto.response.SubscriptionStatisticsResponse stats = subscriptionService.getSubscriptionStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
