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

@Tag(name = "Customer - Free Revenue Vouchers", description = "APIs xem danh sách voucher ưu đãi miễn phí từ doanh thu của MoodCafe (không cần đổi MoodPoint)")
@RestController
@RequestMapping("/api/revenue-vouchers")
@RequiredArgsConstructor
public class RevenueVoucherPublicController {

    private final RevenueVoucherCampaignService campaignService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Xem danh sách chương trình voucher miễn phí đang mở nhận (0 MoodPoints)")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AvailableRevenueVoucherResponse>>> getAvailableCampaigns() {
        UUID userId = null;
        try {
            User current = currentUserService.getCurrentUser();
            if (current != null) {
                userId = current.getUserId();
            }
        } catch (Exception ignored) {
            // Anonymous browsing is allowed
        }
        List<AvailableRevenueVoucherResponse> list = campaignService.getAvailableForCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách voucher miễn phí thành công"));
    }
}

