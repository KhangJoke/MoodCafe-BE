package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.StoreReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreReviewRepository extends JpaRepository<StoreReview, UUID> {

    Page<StoreReview> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    List<StoreReview> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<StoreReview> findTop10ByStoreStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<StoreReview> findAllByUserUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByStoreStoreIdAndUserUserId(UUID storeId, UUID userId);

    @Query("SELECT COUNT(r) > 0 FROM StoreReview r WHERE r.store.storeId = :storeId AND r.user.userId = :userId")
    boolean existsByStoreIdAndUserId(@Param("storeId") UUID storeId, @Param("userId") UUID userId);

    Optional<StoreReview> findFirstByStoreStoreIdAndUserUserIdOrderByCreatedAtDesc(UUID storeId, UUID userId);

    long countByStoreStoreId(UUID storeId);

    @Query("SELECT AVG(r.overallRating), AVG(r.quietnessRating), AVG(r.lightingRating), AVG(r.seatingRating), AVG(r.outletRating), COUNT(r) " +
           "FROM StoreReview r WHERE r.store.storeId = :storeId")
    List<Object[]> getReviewSummaryByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT r.store.storeId, AVG(r.overallRating), COUNT(r) FROM StoreReview r GROUP BY r.store.storeId")
    List<Object[]> findOverallRatingAndCountGroupedByStore();
}
