package com.moodcafe.voucher.controller;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.voucher.dto.CreateRevenueVoucherCampaignRequest;
import com.moodcafe.voucher.dto.RevenueVoucherCampaignResponse;
import com.moodcafe.voucher.dto.UpdateRevenueCampaignBannerRequest;
import com.moodcafe.voucher.dto.UpdateRevenueCampaignStatusRequest;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import com.moodcafe.voucher.service.RevenueVoucherCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/revenue-vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Revenue Voucher Campaigns", description = "Quản trị tạo và theo dõi Voucher từ Doanh thu (User nhận miễn phí, không cần đổi MoodPoint)")
public class AdminRevenueVoucherController {

    private final RevenueVoucherCampaignService campaignService;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "1. Tạo chiến dịch Voucher từ Doanh thu (DIRECT_GRANT phát trực tiếp hoặc CLAIMABLE_POOL user tự bấm nhận)")
    @PostMapping
    public ResponseEntity<ApiResponse<RevenueVoucherCampaignResponse>> createCampaign(
            @Valid @RequestBody CreateRevenueVoucherCampaignRequest request
    ) {
        User admin = currentUserService.getCurrentUser();
        UUID adminId = admin != null ? admin.getUserId() : null;
        RevenueVoucherCampaignResponse created = campaignService.createCampaign(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo chiến dịch voucher từ doanh thu thành công"));
    }

    @Operation(summary = "2. Xem danh sách chiến dịch Voucher từ Doanh thu (lọc theo trạng thái, nguồn doanh thu, loại phân phối)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RevenueVoucherCampaignResponse>>> listCampaigns(
            @RequestParam(required = false) RevenueCampaignStatus status,
            @RequestParam(required = false) RevenueFundingSource fundingSource,
            @RequestParam(required = false) RevenueVoucherDistributionType distributionType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<RevenueVoucherCampaignResponse> page = campaignService.listCampaignsAdmin(
                status, fundingSource, distributionType, search, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách chiến dịch voucher thành công"));
    }

    @Operation(summary = "3. Xem chi tiết một chiến dịch Voucher từ Doanh thu")
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<RevenueVoucherCampaignResponse>> getCampaignDetail(
            @PathVariable UUID campaignId
    ) {
        RevenueVoucherCampaignResponse detail = campaignService.getCampaignDetailAdmin(campaignId);
        return ResponseEntity.ok(ApiResponse.success(detail, "Lấy chi tiết chiến dịch thành công"));
    }

    @Operation(summary = "4. Xem danh sách các voucher đã phát hành thuộc chiến dịch")
    @GetMapping("/{campaignId}/vouchers")
    public ResponseEntity<ApiResponse<PageResponse<CustomerVoucher>>> getCampaignVouchers(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CustomerVoucher> vouchers = campaignService.getCampaignVouchersAdmin(campaignId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(vouchers, "Lấy danh sách voucher của chiến dịch thành công"));
    }

    @Operation(summary = "5. Cập nhật trạng thái chiến dịch (ACTIVE / PAUSED / CANCELLED)")
    @PatchMapping("/{campaignId}/status")
    public ResponseEntity<ApiResponse<RevenueVoucherCampaignResponse>> updateStatus(
            @PathVariable UUID campaignId,
            @Valid @RequestBody UpdateRevenueCampaignStatusRequest request
    ) {
        RevenueVoucherCampaignResponse updated = campaignService.updateCampaignStatus(campaignId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật trạng thái chiến dịch thành công"));
    }

    @Operation(summary = "6. Cập nhật URL ảnh banner cho chiến dịch voucher (để hiển thị thumbnail/carousel trang chủ)")
    @PatchMapping("/{campaignId}/banner")
    public ResponseEntity<ApiResponse<RevenueVoucherCampaignResponse>> updateBannerUrl(
            @PathVariable UUID campaignId,
            @Valid @RequestBody UpdateRevenueCampaignBannerRequest request
    ) {
        RevenueVoucherCampaignResponse updated = campaignService.updateCampaignBanner(campaignId, request.getBannerUrl());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật ảnh banner chiến dịch thành công"));
    }

    @Operation(summary = "7. Upload trực tiếp file ảnh banner lên Cloudinary và gắn vào chiến dịch voucher")
    @PostMapping(value = "/{campaignId}/banner/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RevenueVoucherCampaignResponse>> uploadBanner(
            @PathVariable UUID campaignId,
            @RequestParam("file") MultipartFile file
    ) {
        UploadImageResponse uploaded = fileStorageService.uploadImage(file, "vouchers/campaigns");
        RevenueVoucherCampaignResponse updated = campaignService.updateCampaignBanner(campaignId, uploaded.getImageUrl());
        return ResponseEntity.ok(ApiResponse.success(updated, "Upload ảnh banner chiến dịch thành công"));
    }
}
