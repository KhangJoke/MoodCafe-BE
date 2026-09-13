-- =========================================================
-- V7: RENAME STORE ROLE 'STAFF' TO 'CASHIER'
-- =========================================================

-- 1. Update existing 'STAFF' role in store_roles to 'CASHIER'
UPDATE store_roles
SET name = 'CASHIER',
    description = 'Store cashier'
WHERE name = 'STAFF';

-- 2. Ensure CASHIER exists if STAFF was not present
INSERT INTO store_roles (name, description)
VALUES ('CASHIER', 'Store cashier')
ON CONFLICT (name) DO NOTHING;
