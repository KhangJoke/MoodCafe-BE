package com.moodcafe.gamification.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.gamification.abstraction.repository.QuestRepository;
import com.moodcafe.gamification.abstraction.repository.UserQuestProgressRepository;
import com.moodcafe.gamification.abstraction.repository.UserTitleRepository;
import com.moodcafe.gamification.abstraction.service.CustomerQuestService;
import com.moodcafe.gamification.dto.response.*;
import com.moodcafe.gamification.entity.Quest;
import com.moodcafe.gamification.entity.Title;
import com.moodcafe.gamification.entity.UserQuestProgress;
import com.moodcafe.gamification.entity.UserTitle;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.VoucherTemplate;
import com.moodcafe.loyalty.dto.CustomerWalletResponse;
import com.moodcafe.loyalty.dto.RewardPointCommand;
import com.moodcafe.loyalty.service.CustomerLoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerQuestServiceImpl implements CustomerQuestService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository progressRepository;
    private final UserTitleRepository userTitleRepository;
    private final CustomerVoucherRepository voucherRepository;
    private final CustomerLoyaltyService customerLoyaltyService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CustomerQuestBoardResponse getMyQuestBoard(QuestGroup group, QuestType type, QuestProgressStatus status) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem nhiệm vụ");
        }
        UUID userId = user.getUserId();

        String todayPeriod = LocalDate.now(VIETNAM_ZONE).toString();
        String monthPeriod = YearMonth.now(VIETNAM_ZONE).toString();

        List<Quest> activeQuests = questRepository.findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc();
        List<UserQuestProgress> allProgress = new ArrayList<>();

        for (Quest q : activeQuests) {
            String periodKey = resolvePeriodKey(q.getQuestType(), todayPeriod, monthPeriod);
            UserQuestProgress uqp = progressRepository.findByUserUserIdAndQuestQuestIdAndPeriodKey(userId, q.getQuestId(), periodKey)
                    .orElseGet(() -> progressRepository.save(
                            UserQuestProgress.builder()
                                    .user(user)
                                    .quest(q)
                                    .periodKey(periodKey)
                                    .currentValue(0L)
                                    .status(QuestProgressStatus.IN_PROGRESS)
                                    .build()
                    ));
            allProgress.add(uqp);
        }

        List<QuestProgressItemDto> dailyItems = new ArrayList<>();
        List<QuestProgressItemDto> milestoneItems = new ArrayList<>();

        int dailyCompleted = 0;
        int dailyTotal = 0;
        int claimableTotal = 0;
        int rewardedTotal = 0;

        for (UserQuestProgress p : allProgress) {
            Quest q = p.getQuest();

            if (group != null && q.getQuestGroup() != group) continue;
            if (type != null && q.getQuestType() != type) continue;
            if (status != null && p.getStatus() != status) continue;

            if (p.getStatus() == QuestProgressStatus.CLAIMABLE) claimableTotal++;
            if (p.getStatus() == QuestProgressStatus.REWARDED) rewardedTotal++;

            boolean isDaily = (q.getQuestType() == QuestType.DAILY);
            if (isDaily) {
                dailyTotal++;
                if (p.getStatus() == QuestProgressStatus.REWARDED || p.getStatus() == QuestProgressStatus.CLAIMABLE) {
                    dailyCompleted++;
                }
            }

            QuestProgressItemDto item = toProgressItemDto(p);
            if (isDaily) {
                dailyItems.add(item);
            } else {
                milestoneItems.add(item);
            }
        }

        return CustomerQuestBoardResponse.builder()
                .todayPeriodKey(todayPeriod)
                .monthPeriodKey(monthPeriod)
                .summary(CustomerQuestBoardResponse.QuestSummaryDto.builder()
                        .dailyCompletedCount(dailyCompleted)
                        .dailyTotalCount(dailyTotal)
                        .claimableTotalCount(claimableTotal)
                        .rewardedTotalCount(rewardedTotal)
                        .build())
                .dailyQuests(dailyItems)
                .milestoneQuests(milestoneItems)
                .build();
    }

    @Override
    @Transactional
    public ClaimQuestRewardResponse claimReward(UUID userQuestId) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        UUID userId = user.getUserId();

        UserQuestProgress progress = progressRepository.findByIdWithLock(userQuestId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy nhiệm vụ"));

        if (!progress.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền nhận thưởng nhiệm vụ này");
        }

        if (progress.getStatus() == QuestProgressStatus.REWARDED) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Nhiệm vụ này đã được nhận thưởng trước đó");
        }

        if (progress.getStatus() != QuestProgressStatus.CLAIMABLE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Nhiệm vụ chưa đạt chỉ tiêu hoàn thành để nhận thưởng");
        }

        progress.setStatus(QuestProgressStatus.REWARDED);
        progress.setRewardedAt(Instant.now());
        progressRepository.save(progress);

        Quest quest = progress.getQuest();
        Long pointsAwarded = quest.getRewardPoints() != null ? quest.getRewardPoints() : 0L;

        // 1. Cộng MoodPoints vào ví
        if (pointsAwarded > 0) {
            customerLoyaltyService.creditRewardPoints(RewardPointCommand.builder()
                    .userId(userId)
                    .points(pointsAwarded)
                    .rewardType("QUEST_REWARD")
                    .sourceId(progress.getUserQuestId().toString())
                    .businessKey("QUEST_REWARD_" + progress.getUserQuestId())
                    .description("Thưởng hoàn thành nhiệm vụ: " + quest.getName())
                    .build());
        }

        // 2. Trao Voucher nếu có
        QuestRewardDto.VoucherRewardDto awardedVoucher = null;
        if (quest.getRewardVoucherTemplate() != null) {
            VoucherTemplate vt = quest.getRewardVoucherTemplate();
            String code = generateVoucherCode();
            int days = vt.getValidDays() != null ? vt.getValidDays() : 60;

            CustomerVoucher cv = CustomerVoucher.builder()
                    .userId(userId)
                    .templateId(vt.getTemplateId())
                    .code(code)
                    .faceValueVnd(vt.getFaceValueVnd())
                    .pointCost(0L)
                    .status("AVAILABLE")
                    .expiresAt(Instant.now().plus(days, ChronoUnit.DAYS))
                    .minGrossAmount(vt.getMinGrossAmount() != null ? vt.getMinGrossAmount() : java.math.BigDecimal.ZERO)
                    .build();
            voucherRepository.save(cv);

            awardedVoucher = QuestRewardDto.VoucherRewardDto.builder()
                    .templateId(vt.getTemplateId())
                    .title(vt.getTitle())
                    .faceValueVnd(vt.getFaceValueVnd())
                    .build();
            log.info("Awarded voucher {} to user {} for quest {}", code, userId, quest.getCode());
        }

        // 3. Trao Danh hiệu nếu có
        EquippedTitleDto awardedTitle = null;
        if (quest.getRewardTitle() != null) {
            Title title = quest.getRewardTitle();
            boolean exists = userTitleRepository.findByUserUserIdAndTitleTitleId(userId, title.getTitleId()).isPresent();
            if (!exists) {
                UserTitle ut = UserTitle.builder()
                        .user(user)
                        .title(title)
                        .equipped(false)
                        .unlockedAt(Instant.now())
                        .build();
                userTitleRepository.save(ut);
                log.info("Awarded title {} to user {}", title.getName(), userId);
            }

            awardedTitle = EquippedTitleDto.builder()
                    .titleId(title.getTitleId())
                    .code(title.getCode())
                    .name(title.getName())
                    .tier(title.getTier().name())
                    .colorHex(title.getColorHex())
                    .iconUrl(title.getIconUrl())
                    .build();
        }

        CustomerWalletResponse wallet = customerLoyaltyService.getLoyaltyAccount(userId);
        long currentBalance = wallet != null && wallet.getPointsBalance() != null ? wallet.getPointsBalance() : 0L;

        return ClaimQuestRewardResponse.builder()
                .userQuestId(progress.getUserQuestId())
                .questCode(quest.getCode())
                .questName(quest.getName())
                .claimedAt(progress.getRewardedAt())
                .pointsAwarded(pointsAwarded)
                .currentPointsBalance(currentBalance)
                .voucherAwarded(awardedVoucher)
                .titleUnlocked(awardedTitle)
                .build();
    }

    private String resolvePeriodKey(QuestType type, String todayPeriod, String monthPeriod) {
        if (type == QuestType.DAILY) return todayPeriod;
        if (type == QuestType.MONTHLY) return monthPeriod;
        return "LIFETIME";
    }

    private QuestProgressItemDto toProgressItemDto(UserQuestProgress p) {
        Quest q = p.getQuest();
        long current = p.getCurrentValue() != null ? p.getCurrentValue() : 0L;
        long target = q.getTargetValue() != null && q.getTargetValue() > 0 ? q.getTargetValue() : 1L;
        double pct = Math.min(100.0, Math.round((current * 100.0 / target) * 10.0) / 10.0);

        String unit = switch (q.getRuleType()) {
            case TOTAL_SPEND -> "VND";
            case DISTINCT_MOOD_CHECKIN -> "MOODS";
            case DISTINCT_STORE_CHECKIN -> "STORES";
            default -> "ORDERS";
        };

        QuestRewardDto.VoucherRewardDto vDto = null;
        if (q.getRewardVoucherTemplate() != null) {
            VoucherTemplate vt = q.getRewardVoucherTemplate();
            vDto = QuestRewardDto.VoucherRewardDto.builder()
                    .templateId(vt.getTemplateId())
                    .title(vt.getTitle())
                    .faceValueVnd(vt.getFaceValueVnd())
                    .build();
        }

        EquippedTitleDto tDto = null;
        if (q.getRewardTitle() != null) {
            Title t = q.getRewardTitle();
            tDto = EquippedTitleDto.builder()
                    .titleId(t.getTitleId())
                    .code(t.getCode())
                    .name(t.getName())
                    .tier(t.getTier().name())
                    .colorHex(t.getColorHex())
                    .iconUrl(t.getIconUrl())
                    .build();
        }

        return QuestProgressItemDto.builder()
                .userQuestId(p.getUserQuestId())
                .questId(q.getQuestId())
                .code(q.getCode())
                .name(q.getName())
                .description(q.getDescription())
                .questGroup(q.getQuestGroup())
                .questType(q.getQuestType())
                .ruleType(q.getRuleType())
                .periodKey(p.getPeriodKey())
                .currentValue(current)
                .targetValue(target)
                .progressPercentage(pct)
                .unit(unit)
                .status(p.getStatus())
                .rewards(QuestRewardDto.builder()
                        .moodPoints(q.getRewardPoints())
                        .voucher(vDto)
                        .title(tDto)
                        .build())
                .claimableAt(p.getClaimableAt())
                .rewardedAt(p.getRewardedAt())
                .build();
    }

    private String generateVoucherCode() {
        StringBuilder sb = new StringBuilder("QC-");
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
