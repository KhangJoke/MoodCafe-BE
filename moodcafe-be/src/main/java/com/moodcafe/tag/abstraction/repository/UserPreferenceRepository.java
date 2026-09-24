package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    List<UserPreference> findAllByUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserPreference up SET up.isDeleted = true, up.deletedAt = CURRENT_TIMESTAMP WHERE up.userId = :userId AND up.isDeleted = false")
    void deleteAllByUserId(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT up.tag.tagId, COUNT(up) FROM UserPreference up WHERE up.skipped = false AND up.tag IS NOT NULL GROUP BY up.tag.tagId ORDER BY COUNT(up) DESC")
    List<Object[]> countPreferencesGroupedByTag();
}
