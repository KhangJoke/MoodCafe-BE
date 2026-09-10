package com.moodcafe.notification.controllers;

import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.notification.abstraction.service.NotificationService;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.createConnection(userDetails.user().getUserId());
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(
                notificationService.getNotificationsForUser(userDetails.user().getUserId()),
                "Notifications retrieved successfully");
    }
}