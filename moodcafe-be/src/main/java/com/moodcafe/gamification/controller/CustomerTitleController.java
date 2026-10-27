package com.moodcafe.gamification.controller;

import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import com.moodcafe.gamification.dto.response.EquippedTitleDto;
import com.moodcafe.gamification.dto.response.UserTitleShowcaseResponse;
import com.moodcafe.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Customer - Titles", description = "APIs xem bộ sưu tập danh hiệu và đeo/gỡ danh hiệu")
@RestController
@RequestMapping("/api/me/titles")
@RequiredArgsConstructor
public class CustomerTitleController {

    private final CustomerTitleService customerTitleService;

    @Operation(summary = "Xem bộ sưu tập danh hiệu của tôi (Đã mở khóa và chưa mở khóa)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserTitleShowcaseResponse>> getMyTitleShowcase() {
        UserTitleShowcaseResponse response = customerTitleService.getMyTitleShowcase();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy bộ sưu tập danh hiệu thành công"));
    }

    @Operation(summary = "Đeo một danh hiệu đã mở khóa lên hồ sơ và bài đánh giá")
    @PutMapping("/{titleId}/equip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EquippedTitleDto>> equipTitle(@PathVariable UUID titleId) {
        EquippedTitleDto response = customerTitleService.equipTitle(titleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đeo danh hiệu thành công"));
    }

    @Operation(summary = "Gỡ danh hiệu hiện tại")
    @PutMapping("/unequip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unequipTitle() {
        customerTitleService.unequipTitle();
        return ResponseEntity.ok(ApiResponse.success(null, "Gỡ danh hiệu thành công"));
    }
}
