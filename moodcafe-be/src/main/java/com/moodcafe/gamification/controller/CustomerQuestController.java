package com.moodcafe.gamification.controller;

import com.moodcafe.gamification.abstraction.service.CustomerQuestService;
import com.moodcafe.gamification.dto.response.ClaimQuestRewardResponse;
import com.moodcafe.gamification.dto.response.CustomerQuestBoardResponse;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer - Quests", description = "APIs xem bảng nhiệm vụ và nhận thưởng")
@RestController
@RequestMapping("/api/me/quests")
@RequiredArgsConstructor
public class CustomerQuestController {

    private final CustomerQuestService customerQuestService;

    @Operation(summary = "Xem bảng nhiệm vụ của tôi (Nhiệm vụ hàng ngày & Cột mốc)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerQuestBoardResponse>> getMyQuestBoard(
            @RequestParam(required = false) QuestGroup group,
            @RequestParam(required = false) QuestType type,
            @RequestParam(required = false) QuestProgressStatus status
    ) {
        CustomerQuestBoardResponse response = customerQuestService.getMyQuestBoard(group, type, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy bảng nhiệm vụ thành công"));
    }

    @Operation(summary = "Nhận thưởng khi nhiệm vụ đã hoàn thành (MoodPoints, Voucher, Danh hiệu)")
    @PostMapping("/{userQuestId}/claim")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClaimQuestRewardResponse>> claimReward(
            @PathVariable UUID userQuestId
    ) {
        ClaimQuestRewardResponse response = customerQuestService.claimReward(userQuestId);
        return ResponseEntity.ok(ApiResponse.success(response, "Nhận phần thưởng nhiệm vụ thành công"));
    }
}
