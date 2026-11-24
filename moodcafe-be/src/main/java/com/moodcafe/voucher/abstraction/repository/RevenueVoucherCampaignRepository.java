package com.moodcafe.voucher.abstraction.repository;

import com.moodcafe.voucher.entity.RevenueVoucherCampaign;
import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RevenueVoucherCampaignRepository
        extends JpaRepository<RevenueVoucherCampaign, UUID>, JpaSpecificationExecutor<RevenueVoucherCampaign> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM RevenueVoucherCampaign c WHERE c.campaignId = :campaignId")
    Optional<RevenueVoucherCampaign> findByIdWithLock(@Param("campaignId") UUID campaignId);

    @Query("""
        SELECT c FROM RevenueVoucherCampaign c
        WHERE c.distributionType = com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType.CLAIMABLE_POOL
          AND c.status = com.moodcafe.voucher.entity.enums.RevenueCampaignStatus.ACTIVE
          AND c.issuedQuantity < c.totalQuantity
          AND (c.startsAt IS NULL OR c.startsAt <= :now)
          AND (c.endsAt IS NULL OR c.endsAt > :now)
        ORDER BY c.createdAt DESC
    """)
    List<RevenueVoucherCampaign> findAvailableClaimableCampaigns(@Param("now") Instant now);

    @Query("SELECT COALESCE(SUM(c.allocatedBudgetVnd - c.releasedBudgetVnd), 0) FROM RevenueVoucherCampaign c WHERE c.excludeFromFinance = false")
    BigDecimal sumTotalEffectiveExpenses();

    @Query("""
        SELECT COALESCE(SUM(c.allocatedBudgetVnd - c.releasedBudgetVnd), 0)
        FROM RevenueVoucherCampaign c
        WHERE c.excludeFromFinance = false
          AND c.fundingSource = :fundingSource
    """)
    BigDecimal sumEffectiveExpensesByFundingSource(@Param("fundingSource") RevenueFundingSource fundingSource);

    @Query("""
        SELECT COALESCE(SUM(c.allocatedBudgetVnd - c.releasedBudgetVnd), 0)
        FROM RevenueVoucherCampaign c
        WHERE c.excludeFromFinance = false
          AND c.createdAt >= :from AND c.createdAt < :to
    """)
    BigDecimal sumEffectiveExpensesBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(c.allocatedBudgetVnd - c.releasedBudgetVnd), 0)
        FROM RevenueVoucherCampaign c
        WHERE c.excludeFromFinance = false
          AND c.fundingSource = :fundingSource
          AND c.createdAt >= :from AND c.createdAt < :to
    """)
    BigDecimal sumEffectiveExpensesByFundingSourceBetween(
            @Param("fundingSource") RevenueFundingSource fundingSource,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        SELECT c FROM RevenueVoucherCampaign c
        WHERE c.excludeFromFinance = false
          AND c.createdAt >= :from AND c.createdAt < :to
        ORDER BY c.createdAt ASC
    """)
    List<RevenueVoucherCampaign> findAllCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    Page<RevenueVoucherCampaign> findByStatusOrderByCreatedAtDesc(RevenueCampaignStatus status, Pageable pageable);

    Page<RevenueVoucherCampaign> findByDistributionTypeOrderByCreatedAtDesc(
            RevenueVoucherDistributionType distributionType,
            Pageable pageable
    );
}

