package com.moodcafe.subscription.abstraction.repository;

import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID>, JpaSpecificationExecutor<SubscriptionPayment> {

    Optional<SubscriptionPayment> findByTransactionCode(String transactionCode);

    Page<SubscriptionPayment> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByStatus(SubscriptionPaymentStatus status);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SubscriptionPayment sp WHERE sp.status = 'SUCCESS'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SubscriptionPayment sp WHERE sp.status = 'SUCCESS' AND sp.paidAt >= :since")
    BigDecimal sumRevenueSince(@Param("since") Instant since);

    @Query("SELECT sp.subscriptionPlan.subscriptionPlanId, COALESCE(SUM(sp.amount), 0) " +
           "FROM SubscriptionPayment sp WHERE sp.status = 'SUCCESS' AND sp.subscriptionPlan IS NOT NULL " +
           "GROUP BY sp.subscriptionPlan.subscriptionPlanId")
    List<Object[]> sumRevenueGroupedByPlan();

    @Query("SELECT sp.user.userId, COUNT(sp) FROM SubscriptionPayment sp WHERE sp.status = 'SUCCESS' GROUP BY sp.user.userId")
    List<Object[]> countSuccessfulPaymentsGroupedByUser();
}
