package com.moodcafe.notification.dto.response;

import com.moodcafe.notification.entity.enums.NotificationType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationResponse(
        UUID notificationId,
        String title,
        String content,
        Boolean read,
        NotificationType type,
        String referenceId,
        Instant createdAt
) {
}