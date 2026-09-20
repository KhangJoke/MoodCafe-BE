-- ====================================================================
-- V27: PREVENT DUPLICATE REVIEWS PER USER PER STORE (ACTIVE REVIEWS ONLY)
-- ====================================================================

-- 1. Create a partial unique index on reviews for (user_id, store_id)
--    where is_deleted = false.
--    This ensures that an active review is unique per user per store,
--    while still allowing users who soft-deleted their review to post a new one.
CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_user_store_active 
ON reviews (user_id, store_id) 
WHERE is_deleted = false;
