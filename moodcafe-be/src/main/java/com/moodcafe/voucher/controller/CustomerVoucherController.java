package com.moodcafe.voucher.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.RevenueVoucherCampaignService;
import com.moodcafe.voucher.service.VoucherRedemptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Customer - My Vouchers", description = "APIs đổi điểm lấy voucher và quản lý voucher cá nhân")
@RestController
@RequestMapping("/api/me/vouchers")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final VoucherRedemptionService voucherRedemptionService;
    private final RevenueVoucherCampaignService revenueVoucherCampaignService;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "1. Đổi MoodPoints lấy Voucher (Flow MoodPoint)")
    @PostMapping("/redeem/{templateId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerVoucher>> redeemVoucher(
            @PathVariable UUID templateId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        CustomerVoucher voucher = voucherRedemptionService.redeemVoucher(user.getUserId(), templateId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(voucher, "Đổi voucher thành công"));
    }

    @Operation(summary = "1b. Nhận Voucher miễn phí từ doanh thu MoodCafe (0 MoodPoints, không cần điều kiện đổi)")
    @PostMapping("/claim/{campaignId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerVoucher>> claimRevenueVoucher(
            @PathVariable UUID campaignId
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        CustomerVoucher voucher = revenueVoucherCampaignService.claimVoucher(user.getUserId(), campaignId);
        return ResponseEntity.ok(ApiResponse.success(voucher, "Nhận voucher miễn phí thành công"));
    }

    @Operation(summary = "2. Xem danh sách voucher khả dụng để áp dụng khi đặt bàn")
    @GetMapping("/usable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CustomerVoucher>>> getUsableVouchers() {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        List<CustomerVoucher> usable = customerVoucherRepository.findUsableVouchers(user.getUserId(), Instant.now());
        return ResponseEntity.ok(ApiResponse.success(usable, "Lấy danh sách voucher khả dụng thành công"));
    }

    @Operation(summary = "3. Xem tất cả voucher của tôi (phân trang)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<CustomerVoucher>>> getMyVouchers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerVoucher> vouchers;
        if (status != null && !status.isBlank()) {
            vouchers = customerVoucherRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getUserId(), status.toUpperCase(), pageable);
        } else {
            vouchers = customerVoucherRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(vouchers, "Lấy danh sách voucher thành công"));
    }
}
