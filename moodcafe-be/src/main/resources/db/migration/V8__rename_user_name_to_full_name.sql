-- =========================================================
-- V8: RENAME user_name TO full_name IN users TABLE
-- =========================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'user_name'
    ) THEN
        ALTER TABLE users RENAME COLUMN user_name TO full_name;
    END IF;
END $$;

ALTER TABLE users ALTER COLUMN full_name TYPE VARCHAR(150);
