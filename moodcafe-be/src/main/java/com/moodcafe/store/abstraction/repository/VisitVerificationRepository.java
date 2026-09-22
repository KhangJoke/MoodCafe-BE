package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.VisitVerification;
import com.moodcafe.store.entity.enums.VisitVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitVerificationRepository extends JpaRepository<VisitVerification, UUID> {

    Optional<VisitVerification> findByVisitVerificationIdAndUserUserId(UUID visitVerificationId, UUID userId);

    Optional<VisitVerification> findFirstByStoreStoreIdAndUserUserIdAndStatusAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID storeId,
            UUID userId,
            VisitVerificationStatus status,
            Instant now
    );

    List<VisitVerification> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId);

    List<VisitVerification> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);
}
