package com.moodcafe.gamification.abstraction.repository;

import com.moodcafe.gamification.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TitleRepository extends JpaRepository<Title, UUID> {
    Optional<Title> findByCode(String code);
    List<Title> findAllByActiveTrueAndIsDeletedFalseOrderByCreatedAtAsc();
}
