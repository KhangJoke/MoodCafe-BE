package com.moodcafe.notification.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.notification.abstraction.repository.NotificationRepository;
import com.moodcafe.notification.abstraction.service.NotificationDispatcherService;
import com.moodcafe.notification.abstraction.service.PushNotificationService;
import com.moodcafe.notification.dto.response.NotificationMessage;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.Notification;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.notification.mapper.NotificationMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherServiceImpl implements NotificationDispatcherService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional
    public NotificationResponse dispatch(
            UUID userId,
            String title,
            String message,
            NotificationType type,
            String referenceId,
            String actionUrl
    ) {
        log.info("Dispatching notification to user: {} [type={}, ref={}]", userId, type, referenceId);

        if (!userService.existsById(userId)) {
            log.warn("Cannot dispatch notification: User not found with ID {}", userId);
            throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
        }

        // 1. Persist to PostgreSQL (Source of Truth)
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(message)
                .read(false)
                .type(type)
                .referenceId(referenceId)
                .actionUrl(actionUrl)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(saved);
        NotificationMessage stompMessage = notificationMapper.toMessage(saved);

        // 2. Realtime In-App Delivery via WebSocket STOMP
        String destination = "/topic/notifications/" + userId;
        try {
            messagingTemplate.convertAndSend(destination, stompMessage);
            log.info("Dispatched STOMP notification to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to dispatch STOMP notification to {}: {}", destination, e.getMessage());
        }

        // 3. Background Push Notification (FCM / APNs)
        Map<String, String> pushData = new HashMap<>();
        pushData.put("id", saved.getNotificationId().toString());
        pushData.put("type", type.name());
        if (referenceId != null) {
            pushData.put("referenceId", referenceId);
        }
        if (actionUrl != null) {
            pushData.put("actionUrl", actionUrl);
        }

        try {
            pushNotificationService.sendPushNotification(userId, title, message, pushData);
        } catch (Exception e) {
            log.error("Failed to forward notification to push service for user {}: {}", userId, e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public void dispatchToUsers(
            List<UUID> userIds,
            String title,
            String message,
            NotificationType type,
            String referenceId,
            String actionUrl
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (UUID userId : userIds) {
            try {
                dispatch(userId, title, message, type, referenceId, actionUrl);
            } catch (Exception e) {
                log.error("Failed to dispatch notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void dispatchToAdmins(
            String title,
            String message,
            NotificationType type,
            String referenceId,
            String actionUrl
    ) {
        List<User> admins = userRepository.findAllByRoleNameAndActiveTrue("ADMIN");
        if (admins == null || admins.isEmpty()) {
            log.warn("No active system admins found to receive notification [type={}]", type);
            return;
        }

        List<UUID> adminIds = admins.stream()
                .map(User::getUserId)
                .toList();

        log.info("Multicasting notification [type={}] to {} admin(s)", type, adminIds.size());
        dispatchToUsers(adminIds, title, message, type, referenceId, actionUrl);
    }
}
