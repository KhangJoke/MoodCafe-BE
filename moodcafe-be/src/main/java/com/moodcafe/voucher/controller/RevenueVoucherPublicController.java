package com.moodcafe.voucher.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.voucher.dto.AvailableRevenueVoucherResponse;
import com.moodcafe.voucher.service.RevenueVoucherCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Customer - Free Revenue Vouchers", description = "APIs xem danh sách voucher ưu đãi miễn phí và banner khuyến mãi trang chủ (không cần đổi MoodPoint)")
@RestController
@RequestMapping("/api/revenue-vouchers")
@RequiredArgsConstructor
public class RevenueVoucherPublicController {

    private final RevenueVoucherCampaignService campaignService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Xem danh sách chương trình voucher miễn phí đang mở nhận (0 MoodPoints)")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableRevenueVoucherResponse>>> getAvailableCampaigns() {
        List<AvailableRevenueVoucherResponse> list = campaignService.getAvailableForCustomer(resolveCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách voucher miễn phí thành công"));
    }

    @Operation(summary = "2. Lấy danh sách banner / thumbnail voucher khuyến mãi đang hoạt động cho trang chủ (chỉ trả các chiến dịch có ảnh banner)")
    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<AvailableRevenueVoucherResponse>>> getHomepageBanners() {
        List<AvailableRevenueVoucherResponse> banners = campaignService.getHomepageBanners(resolveCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(banners, "Lấy danh sách banner khuyến mãi thành công"));
    }

    private UUID resolveCurrentUserId() {
        try {
            User current = currentUserService.getCurrentUser();
            if (current != null) {
                return current.getUserId();
            }
        } catch (Exception ignored) {
            // Anonymous browsing is allowed
        }
        return null;
    }
}
