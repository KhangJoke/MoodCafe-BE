package com.moodcafe.gamification.abstraction.repository;

import com.moodcafe.gamification.entity.Quest;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID>, JpaSpecificationExecutor<Quest> {
    Optional<Quest> findByCode(String code);
    List<Quest> findAllByActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc();
    List<Quest> findAllByQuestTypeAndActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(QuestType questType);
    List<Quest> findAllByQuestGroupAndActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(QuestGroup questGroup);
}
