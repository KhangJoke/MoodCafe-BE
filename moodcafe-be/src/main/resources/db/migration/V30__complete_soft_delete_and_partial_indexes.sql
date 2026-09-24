-- =========================================================================
-- V30: COMPLETE SOFT DELETE AND PARTIAL UNIQUE INDEXES FOR ALL ENTITIES
-- =========================================================================

-- 1. Add is_deleted and deleted_at columns to tables missing them
ALTER TABLE tag_categories 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

ALTER TABLE user_preferences 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

ALTER TABLE onboarding_questions 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

ALTER TABLE system_configurations 
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

-- 2. Add deleted_at to remaining tables that already had is_deleted
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE store_images ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE store_staffs ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE store_roles ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE favorite_stores ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE tag_ratings ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE review_images ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE review_reports ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE visit_verifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE vibe_survey_logs ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

-- 3. Replace Full Unique Constraints with Partial Unique Indexes (WHERE is_deleted = false)

-- store_tags: (store_id, tag_id)
ALTER TABLE store_tags DROP CONSTRAINT IF EXISTS uq_store_vibe_tag;
DROP INDEX IF EXISTS uq_store_vibe_tag;
CREATE UNIQUE INDEX IF NOT EXISTS uq_store_vibe_tag_active 
    ON store_tags(store_id, tag_id) 
    WHERE is_deleted = FALSE;

-- user_preferences: (user_id, question_id, tag_id) & (user_id, question_id)
DROP INDEX IF EXISTS uq_user_pref_tag;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_pref_tag_active 
    ON user_preferences(user_id, question_id, tag_id) 
    WHERE tag_id IS NOT NULL AND is_deleted = FALSE;

DROP INDEX IF EXISTS uq_user_pref_slider;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_pref_slider_active 
    ON user_preferences(user_id, question_id) 
    WHERE numeric_value IS NOT NULL AND is_deleted = FALSE;

-- favorite_stores: (user_id, store_id)
ALTER TABLE favorite_stores DROP CONSTRAINT IF EXISTS uq_user_favorite_store;
DROP INDEX IF EXISTS uq_user_favorite_store;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_favorite_store_active 
    ON favorite_stores(user_id, store_id) 
    WHERE is_deleted = FALSE;

-- store_staffs: (store_id, user_id)
ALTER TABLE store_staffs DROP CONSTRAINT IF EXISTS uq_store_staff;
DROP INDEX IF EXISTS uq_store_staff;
CREATE UNIQUE INDEX IF NOT EXISTS uq_store_staff_active 
    ON store_staffs(store_id, user_id) 
    WHERE is_deleted = FALSE;

-- tag_ratings: (review_id, tag_id)
ALTER TABLE tag_ratings DROP CONSTRAINT IF EXISTS uq_review_tag_rating;
DROP INDEX IF EXISTS uq_review_tag_rating;
CREATE UNIQUE INDEX IF NOT EXISTS uq_review_tag_rating_active 
    ON tag_ratings(review_id, tag_id) 
    WHERE is_deleted = FALSE;

-- tags: (name)
ALTER TABLE tags DROP CONSTRAINT IF EXISTS vibe_tags_name_key;
DROP INDEX IF EXISTS vibe_tags_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_name_active 
    ON tags(name) 
    WHERE is_deleted = FALSE;

-- tag_categories: (code) & (name)
ALTER TABLE tag_categories DROP CONSTRAINT IF EXISTS tag_categories_name_key;
ALTER TABLE tag_categories DROP CONSTRAINT IF EXISTS tag_categories_code_key;
DROP INDEX IF EXISTS tag_categories_name_key;
DROP INDEX IF EXISTS tag_categories_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_categories_code_active 
    ON tag_categories(code) 
    WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_categories_name_active 
    ON tag_categories(name) 
    WHERE is_deleted = FALSE;

-- system_configurations: (config_key)
ALTER TABLE system_configurations DROP CONSTRAINT IF EXISTS system_configurations_config_key_key;
DROP INDEX IF EXISTS system_configurations_config_key_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_system_configurations_key_active 
    ON system_configurations(config_key) 
    WHERE is_deleted = FALSE;

-- roles: (name)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_key;
DROP INDEX IF EXISTS roles_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_name_active 
    ON roles(name) 
    WHERE is_deleted = FALSE;

-- store_roles: (name)
ALTER TABLE store_roles DROP CONSTRAINT IF EXISTS store_roles_name_key;
DROP INDEX IF EXISTS store_roles_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_store_roles_name_active 
    ON store_roles(name) 
    WHERE is_deleted = FALSE;

-- 4. Additional Soft Delete Lookup Indexes
CREATE INDEX IF NOT EXISTS idx_store_tags_is_deleted ON store_tags(is_deleted);
CREATE INDEX IF NOT EXISTS idx_user_preferences_is_deleted ON user_preferences(is_deleted);
CREATE INDEX IF NOT EXISTS idx_tag_categories_is_deleted ON tag_categories(is_deleted);
CREATE INDEX IF NOT EXISTS idx_onboarding_questions_is_deleted ON onboarding_questions(is_deleted);
