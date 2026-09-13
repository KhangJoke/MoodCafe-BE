-- =========================================================
-- V5: SPRINT 1 SCHEMA UPDATES
-- User Onboarding, Direct Store Ownership, Master Tag Categories,
-- Tag Ratings and Verification 24h Expiry
-- =========================================================

-- 1. Update USERS table for Onboarding & Noise Tolerance
ALTER TABLE users ADD COLUMN IF NOT EXISTS noise_tolerance VARCHAR(20) DEFAULT 'MEDIUM';
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_first_login BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Update STORES table with direct owner_id
ALTER TABLE stores ADD COLUMN IF NOT EXISTS owner_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_stores_owner'
    ) THEN
        ALTER TABLE stores
            ADD CONSTRAINT fk_stores_owner
            FOREIGN KEY (owner_id)
            REFERENCES users (user_id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- 3. Update VIBE_TAGS table with category (VIBE vs PURPOSE)
ALTER TABLE vibe_tags ADD COLUMN IF NOT EXISTS category VARCHAR(30) NOT NULL DEFAULT 'VIBE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_vibe_tags_category'
    ) THEN
        ALTER TABLE vibe_tags
            ADD CONSTRAINT chk_vibe_tags_category
            CHECK (category IN ('VIBE', 'PURPOSE'));
    END IF;
END $$;

-- 4. Seed Standard Master Tags (Vibes & Purposes)
INSERT INTO vibe_tags (name, description, tag_type, category, is_active)
VALUES
    -- Vibe Tags
    ('Vintage', 'Phong cách hoài niệm, cổ điển ấm cúng', 'PRIMARY', 'VIBE', TRUE),
    ('Minimalist', 'Phong cách tối giản, tinh tế, thoáng đãng', 'PRIMARY', 'VIBE', TRUE),
    ('Industrial', 'Phong cách công xưởng hiện đại, cá tính', 'PRIMARY', 'VIBE', TRUE),
    ('Sân vườn / Botanical', 'Không gian cây xanh nhiệt đới, trong lành', 'PRIMARY', 'VIBE', TRUE),
    ('Hiện đại / Modern', 'Thiết kế sang trọng, thời thượng', 'PRIMARY', 'VIBE', TRUE),

    -- Purpose Tags
    ('Học bài / Chạy deadline', 'Không gian yên tĩnh, bàn cao, ổ điện thuận tiện để tập trung', 'PRIMARY', 'PURPOSE', TRUE),
    ('Hẹn hò lãng mạn', 'Ánh sáng ấm dịu, chỗ ngồi riêng tư, không khí lãng mạn', 'PRIMARY', 'PURPOSE', TRUE),
    ('Làm việc nhóm', 'Bàn rộng, không gian trao đổi thảo luận thoải mái', 'PRIMARY', 'PURPOSE', TRUE),
    ('Chụp ảnh / Sống ảo', 'Nhiều góc check-in đẹp, ánh sáng tự nhiên bắt mắt', 'PRIMARY', 'PURPOSE', TRUE),
    ('Tụ tập bạn bè / Boardgame', 'Không khí vui vẻ, sôi nổi, đồ uống đa dạng', 'PRIMARY', 'PURPOSE', TRUE),
    ('Thư giãn / Đọc sách', 'Nhạc êm dịu, ghế tựa thư thái', 'PRIMARY', 'PURPOSE', TRUE)
ON CONFLICT (name) DO NOTHING;

-- 5. Update VISIT_VERIFICATIONS for BR-09 (24h Expiry)
ALTER TABLE visit_verifications ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours');
ALTER TABLE visit_verifications ADD COLUMN IF NOT EXISTS is_used BOOLEAN NOT NULL DEFAULT FALSE;

-- 6. Create TAG_RATINGS table (Option A Review Mechanism)
CREATE TABLE IF NOT EXISTS tag_ratings (
    tag_rating_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id     UUID NOT NULL,
    vibe_tag_id   UUID NOT NULL,
    score         INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tag_ratings_review
        FOREIGN KEY (review_id)
        REFERENCES reviews (review_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tag_ratings_tag
        FOREIGN KEY (vibe_tag_id)
        REFERENCES vibe_tags (vibe_tag_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_review_tag_rating
        UNIQUE (review_id, vibe_tag_id)
);

CREATE INDEX IF NOT EXISTS idx_tag_ratings_tag_id ON tag_ratings(vibe_tag_id);
