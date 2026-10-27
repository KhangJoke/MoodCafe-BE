package com.moodcafe.gamification.abstraction.repository;

import com.moodcafe.gamification.entity.UserTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTitleRepository extends JpaRepository<UserTitle, UUID> {
    Optional<UserTitle> findByUserUserIdAndTitleTitleId(UUID userId, UUID titleId);
    Optional<UserTitle> findByUserUserIdAndEquippedTrueAndIsDeletedFalse(UUID userId);
    List<UserTitle> findAllByUserUserIdAndIsDeletedFalseOrderByUnlockedAtDesc(UUID userId);
    long countByUserUserIdAndIsDeletedFalse(UUID userId);

    @Modifying
    @Query("UPDATE UserTitle ut SET ut.equipped = false WHERE ut.user.userId = :userId")
    void unequipAllByUserId(@Param("userId") UUID userId);
}
