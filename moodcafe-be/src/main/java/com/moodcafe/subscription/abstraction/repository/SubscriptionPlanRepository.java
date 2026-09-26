package com.moodcafe.subscription.abstraction.repository;

import com.moodcafe.subscription.entity.SubscriptionPlan;
import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByName(String name);

    Optional<SubscriptionPlan> findByPlanCode(SubscriptionPlanCode planCode);

    List<SubscriptionPlan> findAllByActiveTrueOrderByPriceAsc();
}
