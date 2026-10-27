package com.moodcafe.gamification.dto.response;

import com.moodcafe.gamification.entity.enums.QuestGroup;
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
public class QuestAdminResponse {
    private UUID questId;
    private String code;
    private String name;
    private String description;
    private QuestGroup questGroup;
    private QuestType questType;
    private RuleType ruleType;
    private Long targetValue;
    private Long rewardPoints;
    private QuestRewardDto.VoucherRewardDto rewardVoucher;
    private EquippedTitleDto rewardTitle;
    private Integer displayOrder;
    private boolean isActive;
    private boolean isSystem;
    private long totalCompletedUsers;
    private Instant createdAt;
    private Instant updatedAt;
}
