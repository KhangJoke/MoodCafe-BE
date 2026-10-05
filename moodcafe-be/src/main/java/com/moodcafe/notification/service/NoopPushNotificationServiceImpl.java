package com.moodcafe.notification.service;

import com.moodcafe.notification.abstraction.service.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnMissingBean(name = "firebasePushNotificationService")
@Slf4j
public class NoopPushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendPushNotification(UUID userId, String title, String body, Map<String, String> data) {
        log.debug("[PUSH DUMMY] Sending push notification to user {}: title='{}', body='{}', data={}",
                userId, title, body, data);
    }

    @Override
    public void sendMulticastPushNotification(List<UUID> userIds, String title, String body, Map<String, String> data) {
        log.debug("[PUSH DUMMY] Multicasting push notification to {} users: title='{}', body='{}', data={}",
                userIds != null ? userIds.size() : 0, title, body, data);
    }
}
