package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.ReviewReport;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID>, JpaSpecificationExecutor<ReviewReport> {

    boolean existsByReviewReviewIdAndReporterUserId(UUID reviewId, UUID reporterUserId);

    List<ReviewReport> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<ReviewReport> findAllByStatusOrderByCreatedAtDesc(ReviewReportStatus status);

    long countByStatus(ReviewReportStatus status);

    @Query("SELECT r.status, COUNT(r) FROM ReviewReport r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT r.reason, COUNT(r) FROM ReviewReport r GROUP BY r.reason")
    List<Object[]> countGroupedByReason();

    @Query("""
        SELECT r.store.storeId, r.store.name, COUNT(r)
        FROM ReviewReport r
        GROUP BY r.store.storeId, r.store.name
        ORDER BY COUNT(r) DESC
    """)
    List<Object[]> findTopReportedStores(Pageable pageable);
}
