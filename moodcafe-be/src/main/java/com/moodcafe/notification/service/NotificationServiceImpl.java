package com.moodcafe.notification.service;

import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.notification.abstraction.cache.RedisIdempotencyService;
import com.moodcafe.notification.abstraction.cache.RedisOtpService;
import com.moodcafe.notification.abstraction.cache.RedisRateLimitService;
import com.moodcafe.notification.abstraction.repository.NotificationRepository;
import com.moodcafe.notification.abstraction.service.EmailSender;
import com.moodcafe.notification.abstraction.service.NotificationService;
import com.moodcafe.notification.dto.request.OtpNotificationRequest;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.Notification;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.notification.mapper.NotificationMapper;
import com.moodcafe.notification.dto.response.NotificationMessage;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final RedisOtpService otpService;
    private final RedisRateLimitService rateLimitService;
    private final RedisIdempotencyService idempotencyService;

    private final EmailSender emailSender;
    private final UserService userService;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<UUID, List<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void otpNotificationHandler(OtpNotificationRequest request) {

        if (idempotencyService.isProcessed(request.getId())) {
            return;
        }

        String email = request.getOtpRequest().email();
        String otpType = request.getOtpType().toString();
        String otp = request.getOtpRequest().otp();

        if (!rateLimitService.isAllowed(otpType, email)) {
            log.warn("OTP rate limit exceeded for {}", email);
            throw new com.moodcafe.shared.exceptions.AppException(com.moodcafe.shared.error.ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }

        if (!otpService.saveOtp(otpType, email, otp)) {
            throw new com.moodcafe.shared.exceptions.AppException(com.moodcafe.shared.error.ErrorCode.OTP_SAVE_FAILED);
        }

        emailSender.sendOtpEmailAsync(email, otp);

        idempotencyService.markProcessed(request.getId());
    }

    @Override
    public SseEmitter createConnection(UUID userId) {

        SseEmitter emitter = new SseEmitter(1_800_000L);

        List<SseEmitter> userEmitters =
                emitters.computeIfAbsent(
                        userId,
                        key -> new CopyOnWriteArrayList<>()
                );

        userEmitters.add(emitter);

        Runnable removeEmitter = () -> {
            List<SseEmitter> activeEmitters =
                    emitters.get(userId);

            if (activeEmitters != null) {
                activeEmitters.remove(emitter);

                if (activeEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        };

        emitter.onCompletion(removeEmitter);
        emitter.onTimeout(removeEmitter);
        emitter.onError(error -> removeEmitter.run());

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("INIT_CONNECTION")
                            .data("Connected successfully!")
            );
        } catch (IOException e) {
            removeEmitter.run();
        }

        return emitter;
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(
            UUID userId,
            String title,
            String message,
            NotificationType type,
            String referenceId
    ) {
        log.info("Creating notification for user: {}", userId);
        if (!userService.existsById(userId)) {
            log.warn("Cannot create notification: User not found with ID {}", userId);
            throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(message)
                .read(false)
                .type(type)
                .referenceId(referenceId)
                .build();

        // 1. Save to PostgreSQL first (source of truth)
        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved with ID: {} for user: {}", saved.getNotificationId(), userId);

        NotificationResponse response = notificationMapper.toResponse(saved);
        NotificationMessage notificationMessage = notificationMapper.toMessage(saved);

        // 2. Push message through WebSocket STOMP
        String destination = "/topic/notifications/" + userId;
        try {
            messagingTemplate.convertAndSend(destination, notificationMessage);
            log.info("Notification pushed via WebSocket STOMP to destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to push notification via WebSocket to {}: {}", destination, e.getMessage());
        }

        // Backward compatibility for active SSE connections if any
        sendToUser(userId, response);

        return response;
    }

    @Override
    @Transactional
    public void createAndSendNotification(
            UUID targetUserId,
            String title,
            String content,
            NotificationType type,
            String referenceId
    ) {
        createNotification(targetUserId, title, content, type, referenceId);
    }

    @Override
    @Transactional
    public void createAndSendNotification(
            List<UUID> targetUserIds,
            String title,
            String content,
            NotificationType type,
            String referenceId
    ) {

        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }

        for (UUID userId : targetUserIds) {
            try {
                createNotification(
                        userId,
                        title,
                        content,
                        type,
                        referenceId
                );
            } catch (Exception e) {
                log.error(
                        "Failed to send notification to user {}",
                        userId,
                        e
                );
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(
            UUID userId
    ) {

        return notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(
            UUID userId,
            Pageable pageable
    ) {
        if (pageable == null) {
            return getNotificationsForUser(userId);
        }

        return notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .getContent()
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        log.info("Marking notification {} as read for user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            log.warn("Access denied: User {} attempted to mark notification {} belonging to user {}",
                    userId, notificationId, notification.getUserId());
            throw new AppException(ErrorCode.FORBIDDEN, "You do not have permission to modify this notification");
        }

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            log.info("Notification {} marked as read successfully", notificationId);
        }

        return notificationMapper.toResponse(notification);
    }


    private void sendToUser(
            UUID userId,
            NotificationResponse response
    ) {

        List<SseEmitter> userEmitters =
                emitters.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters =
                new ArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            try {

                emitter.send(
                        SseEmitter.event()
                                .name("NEW_NOTIFICATION")
                                .data(response)
                );

            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {

            userEmitters.removeAll(deadEmitters);

            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}