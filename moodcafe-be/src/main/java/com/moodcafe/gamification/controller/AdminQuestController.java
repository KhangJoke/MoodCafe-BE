package com.moodcafe.gamification.controller;

import com.moodcafe.gamification.abstraction.service.AdminQuestService;
import com.moodcafe.gamification.dto.request.CreateQuestAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateQuestAdminRequest;
import com.moodcafe.gamification.dto.response.QuestAdminResponse;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin - Quests Management", description = "APIs quản trị hệ thống nhiệm vụ (Tạo nhiệm vụ ngày/cột mốc thưởng điểm MoodPoints, Voucher, Danh hiệu)")
@RestController
@RequestMapping("/api/admin/quests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminQuestController {

    private final AdminQuestService adminQuestService;

    @Operation(summary = "Danh sách tất cả nhiệm vụ có phân trang và bộ lọc")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestAdminResponse>>> getQuests(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) QuestGroup group,
            @RequestParam(required = false) QuestType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<QuestAdminResponse> response = adminQuestService.getAllQuests(group, type, active, null, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách nhiệm vụ thành công"));
    }

    @Operation(summary = "Xem chi tiết một nhiệm vụ theo ID")
    @GetMapping("/{questId}")
    public ResponseEntity<ApiResponse<QuestAdminResponse>> getQuestById(@PathVariable UUID questId) {
        QuestAdminResponse response = adminQuestService.getQuestById(questId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết nhiệm vụ thành công"));
    }

    @Operation(summary = "Tạo mới nhiệm vụ (Hỗ trợ Daily Quest thưởng điểm như Bing/Edge, Voucher, Title)")
    @PostMapping
    public ResponseEntity<ApiResponse<QuestAdminResponse>> createQuest(
            @Valid @RequestBody CreateQuestAdminRequest request
    ) {
        QuestAdminResponse response = adminQuestService.createQuest(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo nhiệm vụ mới thành công"));
    }

    @Operation(summary = "Cập nhật thông tin cấu hình nhiệm vụ (Chỉ tiêu số lượng targetValue, điểm thưởng rewardPoints, bật/tắt isActive)")
    @PutMapping("/{questId}")
    public ResponseEntity<ApiResponse<QuestAdminResponse>> updateQuest(
            @PathVariable UUID questId,
            @Valid @RequestBody UpdateQuestAdminRequest request
    ) {
        QuestAdminResponse response = adminQuestService.updateQuest(questId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật nhiệm vụ thành công"));
    }

    @Operation(summary = "Bật / tắt nhanh trạng thái kích hoạt của nhiệm vụ (Active / Inactive)")
    @PatchMapping("/{questId}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleQuestStatus(
            @PathVariable UUID questId,
            @RequestParam boolean active
    ) {
        adminQuestService.toggleQuestStatus(questId, active);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái nhiệm vụ thành công"));
    }

    @Operation(summary = "Xóa (vô hiệu hóa/soft-delete) nhiệm vụ (Không cho phép xóa nhiệm vụ hệ thống cố định)")
    @DeleteMapping("/{questId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuest(@PathVariable UUID questId) {
        adminQuestService.deleteQuest(questId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa nhiệm vụ thành công"));
    }
}
