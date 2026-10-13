package com.moodcafe.sponsored.abstraction.repository;

import com.moodcafe.sponsored.entity.SponsoredPlan;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SponsoredPlanRepository extends JpaRepository<SponsoredPlan, UUID> {

    List<SponsoredPlan> findAllByOrderByCreatedAtDesc();

    List<SponsoredPlan> findAllByActiveTrueOrderByPriceAsc();

    List<SponsoredPlan> findAllByPlacementAndActiveTrueOrderByPriceAsc(SponsoredPlacement placement);

    Optional<SponsoredPlan> findFirstByPlacementAndDurationDaysAndActiveTrue(SponsoredPlacement placement, Integer durationDays);

    boolean existsByName(String name);

    boolean existsByNameAndSponsoredPlanIdNot(String name, UUID sponsoredPlanId);
}
