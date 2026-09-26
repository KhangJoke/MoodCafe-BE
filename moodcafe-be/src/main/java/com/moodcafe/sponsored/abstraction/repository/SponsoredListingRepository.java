package com.moodcafe.sponsored.abstraction.repository;

import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SponsoredListingRepository extends JpaRepository<SponsoredListing, UUID> {

    Page<SponsoredListing> findAllByStoreStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    Page<SponsoredListing> findAllByStoreStoreIdAndStatusOrderByCreatedAtDesc(UUID storeId, SponsoredListingStatus status, Pageable pageable);

    @Query("SELECT sl FROM SponsoredListing sl " +
           "WHERE sl.placement = :placement " +
           "AND sl.status = :status " +
           "AND sl.startDate <= :now AND sl.endDate >= :now " +
           "ORDER BY sl.createdAt DESC")
    List<SponsoredListing> findActiveByPlacement(
            @Param("placement") SponsoredPlacement placement,
            @Param("status") SponsoredListingStatus status,
            @Param("now") Instant now
    );

    Optional<SponsoredListing> findByTransactionCode(String transactionCode);

    List<SponsoredListing> findAllByStatusAndEndDateBefore(SponsoredListingStatus status, Instant now);
}
