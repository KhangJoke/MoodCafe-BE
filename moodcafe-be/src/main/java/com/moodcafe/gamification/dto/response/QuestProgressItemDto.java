package com.moodcafe.gamification.dto.response;

import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.gamification.entity.enums.RuleType;
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
public class QuestProgressItemDto {
    private UUID userQuestId;
    private UUID questId;
    private String code;
    private String name;
    private String description;
    private QuestGroup questGroup;
    private QuestType questType;
    private RuleType ruleType;
    private String periodKey;
    private Long currentValue;
    private Long targetValue;
    private Double progressPercentage;
    private String unit;
    private QuestProgressStatus status;
    private QuestRewardDto rewards;
    private Instant claimableAt;
    private Instant rewardedAt;
}
