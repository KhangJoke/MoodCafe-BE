package com.moodcafe.finance.abstraction.repository;

import com.moodcafe.finance.entity.CommissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, UUID> {

    @Query("""
        SELECT cp FROM CommissionPolicy cp
        WHERE cp.storeId = :storeId
          AND cp.isActive = true
          AND cp.effectiveFrom <= :now
          AND (cp.effectiveTo IS NULL OR cp.effectiveTo > :now)
        ORDER BY cp.effectiveFrom DESC, cp.createdAt DESC, cp.policyId DESC
    """)
    List<CommissionPolicy> findActiveStorePolicies(@Param("storeId") UUID storeId, @Param("now") Instant now);

    @Query("""
        SELECT cp FROM CommissionPolicy cp
        WHERE cp.storeId IS NULL
          AND cp.isActive = true
          AND cp.effectiveFrom <= :now
          AND (cp.effectiveTo IS NULL OR cp.effectiveTo > :now)
        ORDER BY cp.effectiveFrom DESC, cp.createdAt DESC, cp.policyId DESC
    """)
    List<CommissionPolicy> findActiveGlobalPolicies(@Param("now") Instant now);

    List<CommissionPolicy> findByStoreId(UUID storeId);

    List<CommissionPolicy> findByStoreIdIsNull();

    @Query("SELECT cp FROM CommissionPolicy cp WHERE cp.storeId = :storeId OR cp.storeId IS NULL ORDER BY cp.effectiveFrom DESC, cp.policyId DESC")
    org.springframework.data.domain.Page<CommissionPolicy> findRelevantPolicies(@Param("storeId") UUID storeId, org.springframework.data.domain.Pageable pageable);

}
