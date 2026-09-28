-- =========================================================
-- V33: ADD DYNAMIC FIELDS TO TAG CATEGORIES
-- Supports full dynamic recommendation engine weights
-- and experience primary/secondary roles for Mobile matchers
-- =========================================================

ALTER TABLE tag_categories
    ADD COLUMN IF NOT EXISTS weight DOUBLE PRECISION NOT NULL DEFAULT 0.2;

ALTER TABLE tag_categories
    ADD COLUMN IF NOT EXISTS is_experience_primary BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tag_categories
    ADD COLUMN IF NOT EXISTS is_experience_secondary BOOLEAN NOT NULL DEFAULT FALSE;

-- Initialize baseline weights and experience roles
UPDATE tag_categories SET weight = 0.35, is_experience_secondary = TRUE WHERE code = 'VIBE';
UPDATE tag_categories SET weight = 0.30, is_experience_primary = TRUE WHERE code = 'PURPOSE';
UPDATE tag_categories SET weight = 0.20 WHERE code = 'NOISE';
UPDATE tag_categories SET weight = 0.15 WHERE code = 'AMENITY';
