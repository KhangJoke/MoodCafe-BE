package com.moodcafe.voucher.abstraction.repository;

import com.moodcafe.voucher.entity.CustomerVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, UUID> {

    Optional<CustomerVoucher> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM CustomerVoucher v WHERE v.voucherId = :voucherId")
    Optional<CustomerVoucher> findByIdWithLock(@Param("voucherId") UUID voucherId);

    Page<CustomerVoucher> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status, Pageable pageable);

    Page<CustomerVoucher> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT v FROM CustomerVoucher v
        WHERE v.userId = :userId
          AND v.status = 'AVAILABLE'
          AND v.expiresAt > :now
        ORDER BY v.expiresAt ASC
    """)
    List<CustomerVoucher> findUsableVouchers(@Param("userId") UUID userId, @Param("now") Instant now);

    Optional<CustomerVoucher> findByLockedReservationId(UUID reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM CustomerVoucher v WHERE v.lockedReservationId = :reservationId")
    Optional<CustomerVoucher> findByLockedReservationIdWithLock(@Param("reservationId") UUID reservationId);

    Optional<CustomerVoucher> findByLockedBookingId(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM CustomerVoucher v WHERE v.lockedBookingId = :bookingId")
    Optional<CustomerVoucher> findByLockedBookingIdWithLock(@Param("bookingId") UUID bookingId);

    long countByCampaignIdAndUserId(UUID campaignId, UUID userId);

    long countByCampaignId(UUID campaignId);

    long countByCampaignIdAndStatus(UUID campaignId, String status);

    Page<CustomerVoucher> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    Page<CustomerVoucher> findByCampaignIdAndStatusOrderByCreatedAtDesc(UUID campaignId, String status, Pageable pageable);

    long countBySourceType(String sourceType);

    long countBySourceTypeAndStatus(String sourceType, String status);

    @Query("SELECT COALESCE(SUM(v.faceValueVnd), 0) FROM CustomerVoucher v WHERE v.sourceType = :sourceType AND v.status = :status")
    java.math.BigDecimal sumFaceValueBySourceTypeAndStatus(@Param("sourceType") String sourceType, @Param("status") String status);

    @Query("""
        SELECT COUNT(v) FROM CustomerVoucher v
        WHERE v.sourceType = :sourceType
          AND v.createdAt >= :from AND v.createdAt < :to
    """)
    long countBySourceTypeBetween(@Param("sourceType") String sourceType, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COUNT(v) FROM CustomerVoucher v
        WHERE v.sourceType = :sourceType
          AND v.status = :status
          AND v.usedAt >= :from AND v.usedAt < :to
    """)
    long countBySourceTypeAndStatusUsedBetween(
            @Param("sourceType") String sourceType,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        SELECT COALESCE(SUM(v.faceValueVnd), 0) FROM CustomerVoucher v
        WHERE v.sourceType = :sourceType
          AND v.status = :status
          AND v.usedAt >= :from AND v.usedAt < :to
    """)
    java.math.BigDecimal sumFaceValueBySourceTypeAndStatusUsedBetween(
            @Param("sourceType") String sourceType,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
