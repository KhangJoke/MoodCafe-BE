package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.ReviewReport;
import com.moodcafe.store.entity.enums.ReviewReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {

    boolean existsByReviewReviewIdAndReporterUserId(UUID reviewId, UUID reporterUserId);

    List<ReviewReport> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<ReviewReport> findAllByStatusOrderByCreatedAtDesc(ReviewReportStatus status);
}
