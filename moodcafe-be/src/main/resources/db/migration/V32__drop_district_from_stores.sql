-- V32: Drop district column and associated index from stores table

DROP INDEX IF EXISTS idx_stores_district;

ALTER TABLE stores DROP COLUMN IF EXISTS district;
