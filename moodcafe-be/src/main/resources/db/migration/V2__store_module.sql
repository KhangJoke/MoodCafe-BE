-- =========================================================
-- V2: STORE MODULE MIGRATION
-- =========================================================
-- V1 already contains the required Store schema:
--   stores, store_roles, store_staffs, store_images,
--   amenities, store_amenities, favorite_stores
-- No Store table recreation is needed in V2.
--
-- This migration only introduces a composite index to optimize
-- querying primary images per store.
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_store_images_primary ON store_images(store_id, is_primary);
