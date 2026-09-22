package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.VibeSurveyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VibeSurveyLogRepository extends JpaRepository<VibeSurveyLog, UUID> {

    boolean existsByVisitVerificationVisitVerificationId(UUID visitVerificationId);

    List<VibeSurveyLog> findAllByVisitVerificationVisitVerificationId(UUID visitVerificationId);
}
