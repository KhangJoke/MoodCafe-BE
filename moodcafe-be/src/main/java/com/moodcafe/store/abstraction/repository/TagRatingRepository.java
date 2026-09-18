package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.TagRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TagRatingRepository extends JpaRepository<TagRating, UUID> {

    List<TagRating> findAllByReviewReviewId(UUID reviewId);

    @Query("SELECT tr.tag.tagId, AVG(tr.score), COUNT(tr.tagRatingId) FROM TagRating tr " +
           "WHERE tr.review.store.storeId = :storeId " +
           "AND tr.review.isDeleted = false AND tr.isDeleted = false " +
           "GROUP BY tr.tag.tagId")
    List<Object[]> getAllTagRatingSummariesForStore(@Param("storeId") UUID storeId);

    @Query("SELECT AVG(tr.score), COUNT(tr.tagRatingId) FROM TagRating tr " +
           "WHERE tr.tag.tagId = :tagId AND tr.review.store.storeId = :storeId " +
           "AND tr.review.isDeleted = false AND tr.isDeleted = false")
    List<Object[]> getTagRatingSummaryForStoreTag(@Param("storeId") UUID storeId, @Param("tagId") UUID tagId);
}
