-- =========================================================
-- V6: ALIGN STORE SCHEMA WITH ERD & ADD MERCHANT_STAFF ROLE
-- =========================================================

-- 1. Insert MERCHANT_STAFF system role for store employees
INSERT INTO roles (name)
VALUES ('MERCHANT_STAFF')
ON CONFLICT (name) DO NOTHING;

-- 2. Drop foreign key constraint on owner_id if exists
ALTER TABLE stores
    DROP CONSTRAINT IF EXISTS fk_stores_owner;

-- 3. Drop owner_id column from stores (ownership is fully represented via store_staffs)
ALTER TABLE stores
    DROP COLUMN IF EXISTS owner_id;
