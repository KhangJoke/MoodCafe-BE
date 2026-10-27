package com.moodcafe.gamification.abstraction.repository;

import com.moodcafe.gamification.entity.UserQuestProgress;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {
    Optional<UserQuestProgress> findByUserUserIdAndQuestQuestIdAndPeriodKey(UUID userId, UUID questId, String periodKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uqp FROM UserQuestProgress uqp WHERE uqp.userQuestId = :id")
    Optional<UserQuestProgress> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uqp FROM UserQuestProgress uqp WHERE uqp.user.userId = :userId AND uqp.quest.questId = :questId AND uqp.periodKey = :periodKey")
    Optional<UserQuestProgress> findByUserAndQuestAndPeriodWithLock(
            @Param("userId") UUID userId,
            @Param("questId") UUID questId,
            @Param("periodKey") String periodKey
    );

    List<UserQuestProgress> findAllByUserUserIdAndPeriodKey(UUID userId, String periodKey);
    List<UserQuestProgress> findAllByUserUserId(UUID userId);
    long countByQuestQuestIdAndStatus(UUID questId, QuestProgressStatus status);
}
