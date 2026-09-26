package com.moodcafe.subscription.abstraction.repository;

import com.moodcafe.subscription.entity.UserSubscription;
import com.moodcafe.subscription.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findFirstByUserUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SubscriptionStatus status);

    List<UserSubscription> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserUserIdAndStatus(UUID userId, SubscriptionStatus status);

    List<UserSubscription> findAllByStatusAndEndDateBefore(SubscriptionStatus status, Instant now);
}
