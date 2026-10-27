package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.gamification.dto.response.ClaimQuestRewardResponse;
import com.moodcafe.gamification.dto.response.CustomerQuestBoardResponse;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import com.moodcafe.gamification.entity.enums.QuestType;

import java.util.UUID;

public interface CustomerQuestService {
    CustomerQuestBoardResponse getMyQuestBoard(QuestGroup group, QuestType type, QuestProgressStatus status);
    ClaimQuestRewardResponse claimReward(UUID userQuestId);
}
