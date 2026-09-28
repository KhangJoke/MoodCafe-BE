package com.moodcafe.subscription.abstraction.repository;

import com.moodcafe.subscription.entity.UserSubscription;
import com.moodcafe.subscription.entity.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID>, JpaSpecificationExecutor<UserSubscription> {

    Optional<UserSubscription> findFirstByUserUserIdAndStatusOrderByCreatedAtDesc(UUID userId, SubscriptionStatus status);

    List<UserSubscription> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserUserIdAndStatus(UUID userId, SubscriptionStatus status);

    List<UserSubscription> findAllByStatusAndEndDateBefore(SubscriptionStatus status, Instant now);

    Page<UserSubscription> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT COUNT(DISTINCT us.user.userId) FROM UserSubscription us")
    long countDistinctUsersWithSubscriptions();

    @Query("SELECT COUNT(DISTINCT us.user.userId) FROM UserSubscription us WHERE us.status = 'ACTIVE'")
    long countDistinctActiveUsers();

    @Query("SELECT us.subscriptionPlan.subscriptionPlanId, COUNT(us) FROM UserSubscription us WHERE us.status = 'ACTIVE' GROUP BY us.subscriptionPlan.subscriptionPlanId")
    List<Object[]> countActiveUsersGroupedByPlan();
}
