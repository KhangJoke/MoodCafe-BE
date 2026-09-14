package com.moodcafe.notification.controller;

import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.notification.abstraction.service.NotificationService;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Deprecated
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.createConnection(userDetails.user().getUserId());
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(
                notificationService.getNotificationsForUser(userDetails.user().getUserId(), pageable),
                "Notifications retrieved successfully");
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID notificationId) {
        NotificationResponse updated = notificationService.markAsRead(
                notificationId,
                userDetails.user().getUserId()
        );
        return ApiResponse.success(updated, "Notification marked as read");
    }
}