package com.moodcafe.notification.abstraction.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PushNotificationService {

    void sendPushNotification(UUID userId, String title, String body, Map<String, String> data);

    void sendMulticastPushNotification(List<UUID> userIds, String title, String body, Map<String, String> data);
}
