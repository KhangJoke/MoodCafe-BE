package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.gamification.dto.request.CreateQuestAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateQuestAdminRequest;
import com.moodcafe.gamification.dto.response.QuestAdminResponse;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminQuestService {
    PageResponse<QuestAdminResponse> getAllQuests(QuestGroup group, QuestType type, Boolean isActive, String search, Pageable pageable);
    QuestAdminResponse getQuestById(UUID questId);
    QuestAdminResponse createQuest(CreateQuestAdminRequest request);
    QuestAdminResponse updateQuest(UUID questId, UpdateQuestAdminRequest request);
    void toggleQuestStatus(UUID questId, boolean active);
    void deleteQuest(UUID questId);
}
