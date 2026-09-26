package com.moodcafe.sponsored.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.sponsored.abstraction.service.SponsoredListingService;
import com.moodcafe.sponsored.dto.request.CreateSponsoredListingRequest;
import com.moodcafe.sponsored.dto.response.SponsoredCheckoutResponse;
import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPricingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredStoreItemResponse;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sponsored")
@RequiredArgsConstructor
@Tag(name = "Sponsored Listing Hub", description = "Quảng bá chi nhánh ưu tiên (Top Banner VIP, Quán mới nổi bật, Cuối tuần Hot, Xu hướng gần bạn)")
public class SponsoredListingController {

    private final SponsoredListingService sponsoredListingService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Lấy bảng giá 4 vị trí tài trợ & kiểm tra quyền hạn gói (Basic bị khóa, PRO/PREMIUM mở khóa)")
    @GetMapping("/pricing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SponsoredPricingResponse>> getPricingAndEligibility(
            @RequestParam(required = false) UUID storeId
    ) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        SponsoredPricingResponse response = sponsoredListingService.getPricingAndEligibility(currentUserId, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Chủ quán đăng ký vị trí tài trợ cho chi nhánh (Hỗ trợ lượt miễn phí gói Premium hoặc thanh toán VNPay/MoMo)")
    @PostMapping("/campaigns")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SponsoredCheckoutResponse>> registerCampaign(
            @Valid @RequestBody CreateSponsoredListingRequest request
    ) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        SponsoredCheckoutResponse response = sponsoredListingService.registerCampaign(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @Operation(summary = "Xác nhận thanh toán tài trợ thành công (Mô phỏng Webhook / Callback từ cổng thanh toán)")
    @PostMapping("/campaigns/{transactionCode}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SponsoredListingResponse>> confirmPayment(
            @PathVariable String transactionCode
    ) {
        SponsoredListingResponse response = sponsoredListingService.confirmCampaignPayment(transactionCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Kích hoạt chiến dịch tài trợ thành công!"));
    }

    @Operation(summary = "Chủ quán xem danh sách chiến dịch đang chạy & lịch sử của chi nhánh (Có đồng hồ đếm ngược, view/click)")
    @GetMapping("/campaigns/store/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<SponsoredListingResponse>>> getStoreCampaigns(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SponsoredListingResponse> response = sponsoredListingService.getStoreCampaigns(storeId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Chủ quán hủy chiến dịch quảng bá (Cho phép khi chiến dịch ở trạng thái PENDING hoặc ACTIVE)")
    @PostMapping("/campaigns/{campaignId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SponsoredListingResponse>> cancelCampaign(
            @RequestParam UUID storeId,
            @PathVariable UUID campaignId
    ) {
        SponsoredListingResponse response = sponsoredListingService.cancelCampaign(storeId, campaignId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hủy chiến dịch thành công"));
    }

    @Operation(summary = "Khách hàng xem danh sách các quán được tài trợ theo vị trí (Top Banner VIP, New Opening, Weekend Picks, Trending)")
    @GetMapping("/placements/{placement}")
    public ResponseEntity<ApiResponse<List<SponsoredStoreItemResponse>>> getActiveStoresByPlacement(
            @PathVariable SponsoredPlacement placement
    ) {
        List<SponsoredStoreItemResponse> response = sponsoredListingService.getActiveStoresByPlacement(placement);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Ghi nhận lượt xem (Impression) chiến dịch tài trợ")
    @PostMapping("/campaigns/{campaignId}/impression")
    public ResponseEntity<ApiResponse<Void>> recordImpression(
            @PathVariable UUID campaignId
    ) {
        sponsoredListingService.recordImpression(campaignId);
        return ResponseEntity.ok(ApiResponse.success(null, "Impression recorded"));
    }

    @Operation(summary = "Ghi nhận lượt click chiến dịch tài trợ")
    @PostMapping("/campaigns/{campaignId}/click")
    public ResponseEntity<ApiResponse<Void>> recordClick(
            @PathVariable UUID campaignId
    ) {
        sponsoredListingService.recordClick(campaignId);
        return ResponseEntity.ok(ApiResponse.success(null, "Click recorded"));
    }
}
