package com.moodcafe.notification.service;

import com.moodcafe.auth.abstraction.service.IUserService;
import com.moodcafe.notification.abstraction.cache.IRedisIdempotencyService;
import com.moodcafe.notification.abstraction.cache.IRedisOtpService;
import com.moodcafe.notification.abstraction.cache.IRedisRateLimitService;
import com.moodcafe.notification.abstraction.repository.NotificationRepository;
import com.moodcafe.notification.abstraction.service.IEmailSender;
import com.moodcafe.notification.abstraction.service.INotificationService;
import com.moodcafe.notification.dto.request.OtpNotificationRequest;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.Notification;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final IRedisOtpService otpService;
    private final IRedisRateLimitService rateLimitService;
    private final IRedisIdempotencyService idempotencyService;

    private final IEmailSender emailSender;
    private final IUserService userService;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

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
    public void createAndSendNotification(
            UUID targetUserId,
            String title,
            String content,
            NotificationType type,
            String referenceId
    ) {

        if (!userService.existsById(targetUserId)) {
            throw new RuntimeException(
                    "User not found: " + targetUserId
            );
        }

        Notification notification = Notification.builder()
                .userId(targetUserId)
                .title(title)
                .content(content)
                .read(false)
                .type(type)
                .referenceId(referenceId)
                .build();

        Notification saved =
                notificationRepository.save(notification);

        NotificationResponse response =
                notificationMapper.toResponse(saved);

        sendToUser(targetUserId, response);
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
                createAndSendNotification(
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