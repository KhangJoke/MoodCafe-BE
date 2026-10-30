package com.moodcafe.gamification.controller;

import com.moodcafe.gamification.abstraction.service.AdminTitleService;
import com.moodcafe.gamification.dto.request.CreateTitleAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateTitleAdminRequest;
import com.moodcafe.gamification.dto.response.TitleAdminResponse;
import com.moodcafe.gamification.entity.enums.TitleTier;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin - Titles Management", description = "APIs quản trị danh hiệu (Tạo, sửa, xóa danh hiệu Đồng/Bạc/Vàng, cung cấp danh sách gán vào nhiệm vụ)")
@RestController
@RequestMapping("/api/admin/titles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTitleController {

    private final AdminTitleService adminTitleService;

    @Operation(summary = "Lấy danh sách tất cả danh hiệu có phân trang và bộ lọc (Tier, Active, Search)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TitleAdminResponse>>> getTitles(
            @RequestParam(required = false) TitleTier tier,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        PageResponse<TitleAdminResponse> response = adminTitleService.getAllTitles(tier, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách danh hiệu thành công"));
    }

    @Operation(summary = "Lấy danh sách danh hiệu đang kích hoạt để đổ vào Dropdown chọn phần thưởng cho Nhiệm vụ")
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<TitleAdminResponse>>> getTitlesForSelection() {
        List<TitleAdminResponse> response = adminTitleService.getActiveTitlesForSelection();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách danh hiệu cho dropdown thành công"));
    }

    @Operation(summary = "Xem chi tiết một danh hiệu theo ID")
    @GetMapping("/{titleId}")
    public ResponseEntity<ApiResponse<TitleAdminResponse>> getTitleById(@PathVariable UUID titleId) {
        TitleAdminResponse response = adminTitleService.getTitleById(titleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết danh hiệu thành công"));
    }

    @Operation(summary = "Tạo mới một danh hiệu (Đồng / Bạc / Vàng)")
    @PostMapping
    public ResponseEntity<ApiResponse<TitleAdminResponse>> createTitle(
            @Valid @RequestBody CreateTitleAdminRequest request
    ) {
        TitleAdminResponse response = adminTitleService.createTitle(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo danh hiệu mới thành công"));
    }

    @Operation(summary = "Cập nhật thông tin danh hiệu (Tên, mô tả, bậc, mã màu hex, icon, bật/tắt)")
    @PutMapping("/{titleId}")
    public ResponseEntity<ApiResponse<TitleAdminResponse>> updateTitle(
            @PathVariable UUID titleId,
            @Valid @RequestBody UpdateTitleAdminRequest request
    ) {
        TitleAdminResponse response = adminTitleService.updateTitle(titleId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật danh hiệu thành công"));
    }

    @Operation(summary = "Bật / tắt nhanh trạng thái kích hoạt của danh hiệu")
    @PatchMapping("/{titleId}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleTitleStatus(
            @PathVariable UUID titleId,
            @RequestParam boolean active
    ) {
        adminTitleService.toggleTitleStatus(titleId, active);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái danh hiệu thành công"));
    }

    @Operation(summary = "Xóa mềm một danh hiệu")
    @DeleteMapping("/{titleId}")
    public ResponseEntity<ApiResponse<Void>> deleteTitle(@PathVariable UUID titleId) {
        adminTitleService.deleteTitle(titleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa danh hiệu thành công"));
    }
}

