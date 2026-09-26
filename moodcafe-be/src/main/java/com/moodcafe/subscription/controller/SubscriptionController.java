package com.moodcafe.subscription.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.dto.request.SubscribePlanRequest;
import com.moodcafe.subscription.dto.response.SubscriptionCheckoutResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "Quản lý Gói Dịch Vụ Chủ Quán (Basic, PRO, PREMIUM) & Lịch Sử Thanh Toán")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Xem danh sách các gói dịch vụ có sẵn (Bảng giá Basic, PRO, PREMIUM)")
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAvailablePlans() {
        List<SubscriptionPlanResponse> response = subscriptionService.getAvailablePlans();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Xem gói dịch vụ hiện tại của Chủ quán (Trạng thái, Ngày hết hạn, Số ngày còn lại, Hạn mức chi nhánh)")
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> getCurrentSubscription() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        UserSubscriptionResponse response = subscriptionService.getCurrentUserSubscription(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Xem lịch sử thanh toán & đăng ký gói dịch vụ")
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionPaymentResponse>>> getPaymentHistory(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        PageResponse<SubscriptionPaymentResponse> response = subscriptionService.getPaymentHistory(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Mua hoặc Gia hạn gói dịch vụ (Tích hợp cổng VNPay / MoMo)")
    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SubscriptionCheckoutResponse>> subscribePlan(
            @Valid @RequestBody SubscribePlanRequest request
    ) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        SubscriptionCheckoutResponse response = subscriptionService.subscribePlan(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "Xác nhận thanh toán thành công (Mô phỏng Webhook / Callback từ cổng thanh toán VNPay/MoMo)")
    @PostMapping("/payments/{transactionCode}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> confirmPayment(
            @PathVariable String transactionCode
    ) {
        UserSubscriptionResponse response = subscriptionService.confirmPayment(transactionCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Kích hoạt gói dịch vụ thành công!"));
    }
}
