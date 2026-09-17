-- V21: Add price_from and price_to numeric columns to stores table

-- 1. ADD COLUMNS
ALTER TABLE stores ADD COLUMN IF NOT EXISTS price_from BIGINT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS price_to BIGINT;

-- 2. CREATE COMPOSITE INDEX FOR RANGE SEARCH
CREATE INDEX IF NOT EXISTS idx_stores_price_range ON stores(price_from, price_to);

-- 3. SEED INITIAL PRICE RANGE VALUES FOR EXISTING STORES
UPDATE stores
SET price_from = 30000, price_to = 65000
WHERE price_range LIKE '%30.000%65.000%' OR price_from IS NULL;

UPDATE stores
SET price_from = 35000, price_to = 75000
WHERE price_range LIKE '%35.000%75.000%';

UPDATE stores
SET price_from = 40000, price_to = 85000
WHERE price_range LIKE '%40.000%85.000%';
