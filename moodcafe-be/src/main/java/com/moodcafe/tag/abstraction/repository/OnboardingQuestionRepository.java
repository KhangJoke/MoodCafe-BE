package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.OnboardingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingQuestionRepository extends JpaRepository<OnboardingQuestion, UUID> {

    List<OnboardingQuestion> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<OnboardingQuestion> findAllByOrderByDisplayOrderAsc();
}
