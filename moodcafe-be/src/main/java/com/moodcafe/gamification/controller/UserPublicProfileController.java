package com.moodcafe.gamification.controller;

import com.moodcafe.gamification.abstraction.service.UserPublicProfileService;
import com.moodcafe.gamification.dto.response.UserPublicProfileResponse;
import com.moodcafe.gamification.dto.response.UserPublicReviewResponse;
import com.moodcafe.gamification.dto.response.UserVibeSnapResponse;
import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Public - User Profile", description = "APIs xem hồ sơ công khai của người dùng, danh hiệu, đánh giá và ảnh VibeSnap")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserPublicProfileController {

    private final UserPublicProfileService userPublicProfileService;

    @Operation(summary = "Xem chi tiết hồ sơ công khai của người dùng (Tên, avatar, ngày tham gia, danh hiệu trang bị, bộ sưu tập danh hiệu, thống kê)")
    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<ApiResponse<UserPublicProfileResponse>> getPublicProfile(
            @PathVariable UUID userId
    ) {
        UserPublicProfileResponse response = userPublicProfileService.getPublicProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hồ sơ công khai thành công"));
    }

    @Operation(summary = "Danh sách đánh giá đã đăng của người dùng (hỗ trợ chuyển quán và cuộn tới đúng review)")
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<UserPublicReviewResponse>>> getUserReviews(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserPublicReviewResponse> response = userPublicProfileService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách đánh giá của người dùng thành công"));
    }

    @Operation(summary = "Bộ sưu tập ảnh VibeSnap xác thực đã chụp tại quán của người dùng")
    @GetMapping("/{userId}/vibesnaps")
    public ResponseEntity<ApiResponse<PageResponse<UserVibeSnapResponse>>> getUserVibeSnaps(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserVibeSnapResponse> response = userPublicProfileService.getUserVibeSnaps(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy bộ sưu tập VibeSnap thành công"));
    }
}
