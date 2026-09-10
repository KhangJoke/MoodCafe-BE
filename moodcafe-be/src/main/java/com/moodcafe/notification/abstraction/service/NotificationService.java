package com.moodcafe.notification.abstraction.service;

import com.moodcafe.notification.dto.request.OtpNotificationRequest;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void otpNotificationHandler(OtpNotificationRequest request);

    SseEmitter createConnection(UUID userId);

    NotificationResponse createNotification(UUID userId, String title, String message, NotificationType type, String referenceId);

    void createAndSendNotification(UUID targetUserId, String title, String content, NotificationType type, String referenceId);

    void createAndSendNotification(List<UUID> targetUserIds, String title, String content, NotificationType type, String referenceId);

    List<NotificationResponse> getNotificationsForUser(UUID userId);

    List<NotificationResponse> getNotificationsForUser(UUID userId, Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId, UUID userId);
}
