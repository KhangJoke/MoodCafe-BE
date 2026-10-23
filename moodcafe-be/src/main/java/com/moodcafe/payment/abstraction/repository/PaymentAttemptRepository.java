package com.moodcafe.payment.abstraction.repository;

import com.moodcafe.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    Optional<PaymentAttempt> findByOrderCode(Long orderCode);

    Optional<PaymentAttempt> findByPurposeAndBusinessId(String purpose, String businessId);

    Optional<PaymentAttempt> findByIntentKey(String intentKey);

    Optional<PaymentAttempt> findByActorIdAndPurposeAndIntentKey(UUID actorId, String purpose, String intentKey);

    List<PaymentAttempt> findAllByBusinessTypeAndBusinessIdOrderByCreatedAtDesc(String businessType, String businessId);

    Optional<PaymentAttempt> findFirstByBusinessTypeAndBusinessIdAndStatusInOrderByCreatedAtDesc(
            String businessType, String businessId, Collection<String> statuses);
}
