package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.StoreTag;
import com.moodcafe.tag.entity.enums.StoreTagStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreTagRepository extends JpaRepository<StoreTag, UUID> {

    List<StoreTag> findAllByStoreId(UUID storeId);

    List<StoreTag> findAllByStoreIdAndStatus(UUID storeId, StoreTagStatus status);

    Optional<StoreTag> findByStoreIdAndTagTagId(UUID storeId, UUID tagId);

    List<StoreTag> findAllByStoreIdAndTagCategoryTagCategoryId(UUID storeId, UUID tagCategoryId);

    List<StoreTag> findAllByStatusOrderByCreatedAtDesc(StoreTagStatus status);

    @Query("SELECT COUNT(DISTINCT st.storeId) FROM StoreTag st WHERE st.tag.tagId = :tagId AND st.status = :status")
    long countDistinctStoresByTagIdAndStatus(
            @Param("tagId") UUID tagId,
            @Param("status") StoreTagStatus status
    );

    @Query("""
        SELECT COUNT(DISTINCT st1.storeId)
        FROM StoreTag st1
        JOIN StoreTag st2 ON st1.storeId = st2.storeId
        WHERE st1.tag.tagId = :purposeTagId
          AND st2.tag.tagId = :vibeTagId
          AND st1.status = :status
          AND st2.status = :status
    """)
    long countStoresWithBothTags(
            @Param("purposeTagId") UUID purposeTagId,
            @Param("vibeTagId") UUID vibeTagId,
            @Param("status") StoreTagStatus status
    );

    @Query("SELECT st.tag.tagId, COUNT(DISTINCT st.storeId) FROM StoreTag st WHERE st.status = :status GROUP BY st.tag.tagId")
    List<Object[]> countDistinctStoresGroupedByTag(@Param("status") StoreTagStatus status);
}
