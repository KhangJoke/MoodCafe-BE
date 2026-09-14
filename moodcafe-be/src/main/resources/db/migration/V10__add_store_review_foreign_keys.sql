-- =========================================================
-- V10: ADD STORE AND USER FOREIGN KEYS TO REVIEWS TABLE
-- =========================================================

-- 1. Make visit_verification_id nullable so reviews can be posted directly
ALTER TABLE reviews ALTER COLUMN visit_verification_id DROP NOT NULL;

-- 2. Add store_id and user_id columns
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS store_id UUID;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS user_id UUID;

-- 3. Add foreign keys with CASCADE delete
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_store') THEN
        ALTER TABLE reviews ADD CONSTRAINT fk_reviews_store
            FOREIGN KEY (store_id) REFERENCES stores(store_id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_user') THEN
        ALTER TABLE reviews ADD CONSTRAINT fk_reviews_user
            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;
    END IF;
END $$;

-- 4. Create performance indexes
CREATE INDEX IF NOT EXISTS idx_reviews_store_id ON reviews(store_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_is_deleted ON reviews(is_deleted);
CREATE INDEX IF NOT EXISTS idx_review_images_is_deleted ON review_images(is_deleted);
