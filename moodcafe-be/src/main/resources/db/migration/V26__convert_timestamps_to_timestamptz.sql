-- V26: Convert TIMESTAMP WITHOUT TIME ZONE columns to TIMESTAMPTZ (UTC Standard)

-- 1. roles
ALTER TABLE roles 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 2. users
ALTER TABLE users 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- 3. refresh_tokens
ALTER TABLE refresh_tokens 
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 4. stores
ALTER TABLE stores 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- 5. store_roles
ALTER TABLE store_roles 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 6. store_staffs
ALTER TABLE store_staffs 
    ALTER COLUMN joined_at TYPE TIMESTAMPTZ USING joined_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 7. store_images
ALTER TABLE store_images 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 8. favorite_stores
ALTER TABLE favorite_stores 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 9. reviews
ALTER TABLE reviews 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- 10. review_images
ALTER TABLE review_images 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 11. store_tags
ALTER TABLE store_tags 
    ALTER COLUMN approved_at TYPE TIMESTAMPTZ USING approved_at AT TIME ZONE 'UTC',
    ALTER COLUMN revoked_at TYPE TIMESTAMPTZ USING revoked_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 12. tags
ALTER TABLE tags 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 13. tag_ratings
ALTER TABLE tag_ratings 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 14. notifications
ALTER TABLE notifications 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN read_at TYPE TIMESTAMPTZ USING read_at AT TIME ZONE 'UTC';

-- 15. visit_verifications
ALTER TABLE visit_verifications 
    ALTER COLUMN captured_at TYPE TIMESTAMPTZ USING captured_at AT TIME ZONE 'UTC',
    ALTER COLUMN verified_at TYPE TIMESTAMPTZ USING verified_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'UTC';

-- 16. store_subscriptions
ALTER TABLE store_subscriptions 
    ALTER COLUMN start_date TYPE TIMESTAMPTZ USING start_date AT TIME ZONE 'UTC',
    ALTER COLUMN end_date TYPE TIMESTAMPTZ USING end_date AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 17. subscription_plans
ALTER TABLE subscription_plans 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- 18. vibe_survey_logs
ALTER TABLE vibe_survey_logs 
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
