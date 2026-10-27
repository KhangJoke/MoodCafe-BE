package com.moodcafe.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimQuestRewardResponse {
    private UUID userQuestId;
    private String questCode;
    private String questName;
    private Instant claimedAt;
    private Long pointsAwarded;
    private Long currentPointsBalance;
    private QuestRewardDto.VoucherRewardDto voucherAwarded;
    private EquippedTitleDto titleUnlocked;
}
