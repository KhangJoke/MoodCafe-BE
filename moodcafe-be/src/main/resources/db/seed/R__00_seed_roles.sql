-- =============================================================
-- R__00: SEED SYSTEM ROLES AND STORE ROLES (IDEMPOTENT)
-- Guarantees system roles & store roles exist prior to user/staff seeds.
-- =============================================================

-- 1. System Roles (roles table)
INSERT INTO roles (name)
SELECT r.name
FROM (VALUES 
    ('CUSTOMER'),
    ('ADMIN'),
    ('MERCHANT_STAFF')
) AS r(name)
WHERE NOT EXISTS (
    SELECT 1 FROM roles existing 
    WHERE existing.name = r.name AND existing.is_deleted = FALSE
);

-- 2. Store Roles (store_roles table)
INSERT INTO store_roles (name, description)
SELECT sr.name, sr.description
FROM (VALUES 
    ('OWNER', 'Store owner'),
    ('MANAGER', 'Store manager'),
    ('CASHIER', 'Store cashier')
) AS sr(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM store_roles existing 
    WHERE existing.name = sr.name AND existing.is_deleted = FALSE
);
