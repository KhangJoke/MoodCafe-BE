-- V20: Add district column to stores table and seed districts for active stores

-- 1. ADD DISTRICT COLUMN
ALTER TABLE stores ADD COLUMN IF NOT EXISTS district VARCHAR(100);

-- 2. CREATE INDEX FOR DISTRICT SEARCH
CREATE INDEX IF NOT EXISTS idx_stores_district ON stores(district);

-- 3. UPDATE DISTRICT FOR EXISTING STORES
UPDATE stores 
SET district = 'Quận 1' 
WHERE name LIKE '%District 1%' OR address LIKE '%District 1%' OR address LIKE '%Quận 1%';

UPDATE stores 
SET district = 'TP. Thủ Đức' 
WHERE name LIKE '%Thao Dien%' OR address LIKE '%Thao Dien%' OR address LIKE '%Thảo Điền%';

UPDATE stores 
SET district = 'Phú Nhuận' 
WHERE name LIKE '%Phu Nhuan%' OR address LIKE '%Phu Nhuan%' OR address LIKE '%Phú Nhuận%';

-- Fallback for any remaining null district
UPDATE stores 
SET district = 'Quận 1' 
WHERE district IS NULL;
