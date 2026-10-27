package com.moodcafe.gamification.abstraction.repository;

import com.moodcafe.gamification.entity.QuestAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestAuditLogRepository extends JpaRepository<QuestAuditLog, UUID> {
    boolean existsByQuestIdAndUserIdAndSourceRefId(UUID questId, UUID userId, String sourceRefId);

    @Query("SELECT COUNT(q) > 0 FROM QuestAuditLog q WHERE q.userId = :userId AND q.storeId = :storeId AND q.actionDate = :actionDate")
    boolean existsCheckInToday(
            @Param("userId") UUID userId,
            @Param("storeId") UUID storeId,
            @Param("actionDate") LocalDate actionDate
    );

    @Query("SELECT DISTINCT q.storeId FROM QuestAuditLog q WHERE q.userId = :userId AND q.periodKey = :periodKey AND q.questId = :questId")
    List<UUID> findDistinctStoresByPeriod(
            @Param("userId") UUID userId,
            @Param("questId") UUID questId,
            @Param("periodKey") String periodKey
    );
}
