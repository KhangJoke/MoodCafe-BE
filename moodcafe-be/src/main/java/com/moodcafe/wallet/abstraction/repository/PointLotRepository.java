package com.moodcafe.wallet.abstraction.repository;

import com.moodcafe.wallet.entity.PointLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PointLotRepository extends JpaRepository<PointLot, UUID> {

    @Query("""
        SELECT l FROM PointLot l
        WHERE l.userId = :userId
          AND l.remainingPoints > 0
          AND (l.expiresAt IS NULL OR l.expiresAt > :now)
        ORDER BY 
          CASE WHEN l.sourceType = 'REFUND' THEN 0 ELSE 1 END ASC,
          CASE WHEN l.sourceType = 'REFUND' THEN l.createdAt END ASC,
          CASE WHEN l.sourceType = 'REWARD' THEN l.expiresAt END ASC
    """)
    List<PointLot> findSpendableLotsOrdered(@Param("userId") UUID userId, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM PointLot l
        WHERE l.userId = :userId
          AND l.remainingPoints > 0
          AND (l.expiresAt IS NULL OR l.expiresAt > :now)
        ORDER BY 
          CASE WHEN l.sourceType = 'REFUND' THEN 0 ELSE 1 END ASC,
          CASE WHEN l.sourceType = 'REFUND' THEN l.createdAt END ASC,
          CASE WHEN l.sourceType = 'REWARD' THEN l.expiresAt END ASC
    """)
    List<PointLot> findSpendableLotsWithLock(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT COALESCE(SUM(l.remainingPoints), 0) FROM PointLot l WHERE l.userId = :userId AND l.remainingPoints > 0 AND (l.expiresAt IS NULL OR l.expiresAt > :now)")
    Long sumActivePointsByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    List<PointLot> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
