-- V29: Drop redundant legacy price_range column from stores

-- 1. Ensure all existing stores have valid numeric price_from and price_to
UPDATE stores
SET price_from = 30000, price_to = 65000
WHERE price_from IS NULL OR price_to IS NULL;

-- 2. Drop the redundant text column
ALTER TABLE stores DROP COLUMN IF EXISTS price_range;
