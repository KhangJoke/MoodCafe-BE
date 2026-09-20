-- ====================================================================
-- V27: PREVENT DUPLICATE REVIEWS PER USER PER STORE (ACTIVE REVIEWS ONLY)
-- ====================================================================

-- 1. Deduplicate any existing active duplicate reviews before applying unique index
--    Keep the most recent review (by created_at, then review_id) and delete older duplicates.
--    Foreign keys review_images and tag_ratings will cascade delete.
DELETE FROM reviews r1
USING reviews r2
WHERE r1.user_id = r2.user_id
  AND r1.store_id = r2.store_id
  AND r1.is_deleted = false
  AND r2.is_deleted = false
  AND (r1.created_at < r2.created_at OR (r1.created_at = r2.created_at AND r1.review_id < r2.review_id));

-- 2. Create a partial unique index on reviews for (user_id, store_id)
--    where is_deleted = false.
--    This ensures that an active review is unique per user per store,
--    while still allowing users who soft-deleted their review to post a new one.
CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_user_store_active 
ON reviews (user_id, store_id) 
WHERE is_deleted = false;
