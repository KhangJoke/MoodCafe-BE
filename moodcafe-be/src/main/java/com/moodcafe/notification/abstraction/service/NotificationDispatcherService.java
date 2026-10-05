package com.moodcafe.notification.abstraction.service;

import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationDispatcherService {

    NotificationResponse dispatch(UUID userId, String title, String message, NotificationType type, String referenceId, String actionUrl);

    void dispatchToUsers(List<UUID> userIds, String title, String message, NotificationType type, String referenceId, String actionUrl);

    void dispatchToAdmins(String title, String message, NotificationType type, String referenceId, String actionUrl);
}
