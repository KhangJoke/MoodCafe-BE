package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    List<UserPreference> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
