package com.moodcafe.notification.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationMessage(
        UUID id,
        String title,
        String message,
        String type,
        Instant createdAt
) {
}
