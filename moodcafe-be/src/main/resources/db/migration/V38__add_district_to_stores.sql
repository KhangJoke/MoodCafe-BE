-- V38: Add district column back to stores table
ALTER TABLE stores ADD COLUMN IF NOT EXISTS district VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_stores_district ON stores(district);

-- Backfill district for existing stores from address if district is null
UPDATE stores
SET district = 'Quận 1'
WHERE district IS NULL AND (address ILIKE '%quận 1%' OR address ILIKE '%district 1%');

UPDATE stores
SET district = 'TP. Thủ Đức'
WHERE district IS NULL AND (address ILIKE '%thủ đức%' OR address ILIKE '%thảo điền%');

UPDATE stores
SET district = 'Bình Thạnh'
WHERE district IS NULL AND address ILIKE '%bình thạnh%';

UPDATE stores
SET district = 'Phú Nhuận'
WHERE district IS NULL AND address ILIKE '%phú nhuận%';

UPDATE stores
SET district = 'Gò Vấp'
WHERE district IS NULL AND address ILIKE '%gò vấp%';

UPDATE stores
SET district = 'Tân Bình'
WHERE district IS NULL AND address ILIKE '%tân bình%';

UPDATE stores
SET district = 'Quận 3'
WHERE district IS NULL AND (address ILIKE '%quận 3%' OR address ILIKE '%district 3%');

UPDATE stores
SET district = 'Quận 7'
WHERE district IS NULL AND (address ILIKE '%quận 7%' OR address ILIKE '%district 7%');
