package com.moodcafe.gamification.service;

import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.entity.Booking;
import com.moodcafe.gamification.abstraction.repository.QuestAuditLogRepository;
import com.moodcafe.gamification.abstraction.repository.QuestRepository;
import com.moodcafe.gamification.abstraction.repository.UserQuestProgressRepository;
import com.moodcafe.gamification.abstraction.service.QuestEventService;
import com.moodcafe.gamification.entity.Quest;
import com.moodcafe.gamification.entity.QuestAuditLog;
import com.moodcafe.gamification.entity.UserQuestProgress;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.gamification.entity.enums.RuleType;
import com.moodcafe.notification.abstraction.service.NotificationService;
import com.moodcafe.notification.entity.enums.NotificationType;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestEventServiceImpl implements QuestEventService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository progressRepository;
    private final QuestAuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void onBookingCheckedIn(Booking booking) {
        if (booking == null || booking.getUser() == null) return;

        User user = booking.getUser();
        UUID userId = user.getUserId();
        Store store = booking.getStore();
        UUID storeId = store != null ? store.getStoreId() : null;
        LocalDate actionDate = LocalDate.now(VIETNAM_ZONE);
        String sourceRefId = booking.getBookingId().toString();

        boolean storeVisitedToday = storeId != null && auditLogRepository.existsCheckInToday(userId, storeId, actionDate);

        List<Quest> activeQuests = questRepository.findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc();

        for (Quest q : activeQuests) {
            try {
                String periodKey = resolvePeriodKey(q.getQuestType());

                if (auditLogRepository.existsByQuestIdAndUserIdAndSourceRefId(q.getQuestId(), userId, sourceRefId)) {
                    continue; // Idempotency
                }

                UserQuestProgress progress = getOrCreateProgress(user, q, periodKey);
                if (progress.getStatus() != QuestProgressStatus.IN_PROGRESS) {
                    continue;
                }

                long delta = 0L;
                RuleType rule = q.getRuleType();

                if (rule == RuleType.FIRST_CHECKIN) {
                    delta = 1L;
                } else if (rule == RuleType.ONTIME_STREAK) {
                    // Anti-farming: only 1 check-in per store per day increments punctuality streak
                    if (!storeVisitedToday) {
                        delta = 1L;
                    }
                } else if (rule == RuleType.DISTINCT_STORE_CHECKIN) {
                    if (storeId != null) {
                        List<UUID> visited = auditLogRepository.findDistinctStoresByPeriod(userId, q.getQuestId(), periodKey);
                        if (!visited.contains(storeId)) {
                            delta = 1L;
                        }
                    }
                } else if (rule == RuleType.DISTINCT_MOOD_CHECKIN) {
                    if (!storeVisitedToday) {
                        delta = 1L;
                    }
                } else if (rule == RuleType.TOTAL_SPEND) {
                    long spend = booking.getGrossAmount() != null ? booking.getGrossAmount().longValue() : 0L;
                    if (spend > 0) {
                        delta = spend;
                    }
                } else if (rule == RuleType.DAILY_ACTION && q.getQuestType() == QuestType.DAILY) {
                    delta = 1L;
                }

                if (delta > 0) {
                    applyProgressDelta(progress, delta, sourceRefId, storeId, actionDate);
                }
            } catch (Exception e) {
                log.error("Failed processing quest {} for booking {}", q.getCode(), booking.getBookingId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void onBookingTableExpired(Booking booking) {
        if (booking == null || booking.getUser() == null) return;
        UUID userId = booking.getUser().getUserId();

        // Reset streak for all in-progress streak quests
        List<Quest> streakQuests = questRepository.findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc().stream()
                .filter(q -> q.getRuleType() == RuleType.ONTIME_STREAK)
                .toList();

        for (Quest q : streakQuests) {
            String periodKey = resolvePeriodKey(q.getQuestType());
            progressRepository.findByUserAndQuestAndPeriodWithLock(userId, q.getQuestId(), periodKey)
                    .ifPresent(p -> {
                        if (p.getStatus() == QuestProgressStatus.IN_PROGRESS && p.getCurrentValue() > 0) {
                            log.info("Resetting punctuality streak for user {} on quest {} due to expired booking {}",
                                    userId, q.getCode(), booking.getBookingId());
                            p.setCurrentValue(0L);
                            progressRepository.save(p);
                        }
                    });
        }
    }

    @Override
    @Transactional
    public void onBookingClaimedLate(Booking booking) {
        if (booking == null || booking.getUser() == null) return;
        User user = booking.getUser();
        UUID userId = user.getUserId();
        Store store = booking.getStore();
        UUID storeId = store != null ? store.getStoreId() : null;
        LocalDate actionDate = LocalDate.now(VIETNAM_ZONE);
        String sourceRefId = "LATE_" + booking.getBookingId().toString();

        // Late booking still counts toward TOTAL_SPEND
        List<Quest> spendQuests = questRepository.findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc().stream()
                .filter(q -> q.getRuleType() == RuleType.TOTAL_SPEND)
                .toList();

        for (Quest q : spendQuests) {
            try {
                String periodKey = resolvePeriodKey(q.getQuestType());
                if (auditLogRepository.existsByQuestIdAndUserIdAndSourceRefId(q.getQuestId(), userId, sourceRefId)) {
                    continue;
                }

                UserQuestProgress progress = getOrCreateProgress(user, q, periodKey);
                if (progress.getStatus() != QuestProgressStatus.IN_PROGRESS) {
                    continue;
                }

                long spend = booking.getGrossAmount() != null ? booking.getGrossAmount().longValue() : 0L;
                if (spend > 0) {
                    applyProgressDelta(progress, spend, sourceRefId, storeId, actionDate);
                }
            } catch (Exception e) {
                log.error("Failed processing spend quest {} for late booking {}", q.getCode(), booking.getBookingId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void onReviewSubmitted(StoreReview review) {
        if (review == null || review.getUser() == null) return;
        User user = review.getUser();
        UUID userId = user.getUserId();
        Store store = review.getStore();
        UUID storeId = store != null ? store.getStoreId() : null;
        LocalDate actionDate = LocalDate.now(VIETNAM_ZONE);
        String sourceRefId = review.getReviewId().toString();

        int charCount = review.getContent() != null ? review.getContent().trim().length() : 0;
        int imgCount = review.getImages() != null ? review.getImages().size() : 0;

        List<Quest> reviewQuests = questRepository.findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc().stream()
                .filter(q -> q.getRuleType() == RuleType.VERIFIED_REVIEW)
                .toList();

        for (Quest q : reviewQuests) {
            try {
                String periodKey = resolvePeriodKey(q.getQuestType());
                if (auditLogRepository.existsByQuestIdAndUserIdAndSourceRefId(q.getQuestId(), userId, sourceRefId)) {
                    continue;
                }

                // Check quest-specific text & image conditions
                if (q.getCode().contains("CRITIC") || q.getCode().contains("INFLUENCER")) {
                    if (charCount < 50 || imgCount < 1) continue;
                } else if (q.getCode().contains("FIRST")) {
                    if (charCount < 30 || imgCount < 1) continue;
                } else if (q.getCode().contains("VERIFIED") || q.getCode().contains("WRITER") || q.getCode().contains("SNAP")) {
                    if (imgCount < 1) continue;
                }

                UserQuestProgress progress = getOrCreateProgress(user, q, periodKey);
                if (progress.getStatus() != QuestProgressStatus.IN_PROGRESS) {
                    continue;
                }

                applyProgressDelta(progress, 1L, sourceRefId, storeId, actionDate);
            } catch (Exception e) {
                log.error("Failed processing review quest {} for review {}", q.getCode(), review.getReviewId(), e);
            }
        }
    }

    private UserQuestProgress getOrCreateProgress(User user, Quest quest, String periodKey) {
        return progressRepository.findByUserAndQuestAndPeriodWithLock(user.getUserId(), quest.getQuestId(), periodKey)
                .orElseGet(() -> progressRepository.save(
                        UserQuestProgress.builder()
                                .user(user)
                                .quest(quest)
                                .periodKey(periodKey)
                                .currentValue(0L)
                                .status(QuestProgressStatus.IN_PROGRESS)
                                .build()
                ));
    }

    private void applyProgressDelta(UserQuestProgress progress, long delta, String sourceRefId, UUID storeId, LocalDate actionDate) {
        Quest q = progress.getQuest();
        long current = progress.getCurrentValue() != null ? progress.getCurrentValue() : 0L;
        long newVal = current + delta;
        long target = q.getTargetValue() != null && q.getTargetValue() > 0 ? q.getTargetValue() : 1L;

        progress.setCurrentValue(newVal);

        if (newVal >= target) {
            progress.setStatus(QuestProgressStatus.CLAIMABLE);
            progress.setClaimableAt(Instant.now());
            log.info("Quest {} completed by user {}! Status set to CLAIMABLE.", q.getCode(), progress.getUser().getUserId());

            notificationService.createAndSendNotification(
                    progress.getUser().getUserId(),
                    "Nhiệm vụ hoàn thành!",
                    "Chúc mừng! Bạn vừa hoàn thành nhiệm vụ [" + q.getName() + "]. Hãy bấm để nhận thưởng ngay!",
                    NotificationType.QUEST_CLAIMABLE,
                    progress.getUserQuestId().toString(),
                    "/quests"
            );
        }

        progressRepository.save(progress);

        QuestAuditLog auditLog = QuestAuditLog.builder()
                .questId(q.getQuestId())
                .userId(progress.getUser().getUserId())
                .periodKey(progress.getPeriodKey())
                .sourceRefId(sourceRefId)
                .storeId(storeId)
                .actionDate(actionDate)
                .valueAdded(delta)
                .build();
        auditLogRepository.save(auditLog);
    }

    private String resolvePeriodKey(QuestType type) {
        if (type == QuestType.DAILY) return LocalDate.now(VIETNAM_ZONE).toString();
        if (type == QuestType.MONTHLY) return YearMonth.now(VIETNAM_ZONE).toString();
        return "LIFETIME";
    }
}
