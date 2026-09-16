-- =========================================================
-- V9: STANDARDIZED TAG SYSTEM & DYNAMIC ONBOARDING
-- Refactor to Tag Categories, Standard Tags with Scale Values,
-- Store Tags, Dynamic Onboarding Questions (Slider & Multi-Select),
-- and User Preferences
-- =========================================================

-- 1. Create TAG_CATEGORIES table
CREATE TABLE IF NOT EXISTS tag_categories (
    tag_category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    approval_mode   VARCHAR(30)  NOT NULL,
    control_type    VARCHAR(30)  NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 1,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tag_category_approval_mode
        CHECK (approval_mode IN ('OWNER_REQUEST', 'OWNER_CUSTOM')),

    CONSTRAINT chk_tag_category_control_type
        CHECK (control_type IN ('TAG_LIST', 'SLIDER'))
);

CREATE INDEX IF NOT EXISTS idx_tag_categories_code ON tag_categories(code);
CREATE INDEX IF NOT EXISTS idx_tag_categories_active ON tag_categories(is_active);

-- Seed Default Tag Categories
INSERT INTO tag_categories (name, code, approval_mode, control_type, display_order, is_active)
VALUES
    ('Độ yên tĩnh & Âm thanh', 'NOISE', 'OWNER_CUSTOM', 'SLIDER', 1, TRUE),
    ('Mục đích sử dụng', 'PURPOSE', 'OWNER_REQUEST', 'TAG_LIST', 2, TRUE),
    ('Vibe & Phong cách', 'VIBE', 'OWNER_REQUEST', 'TAG_LIST', 3, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 2. Rename vibe_tags to TAGS and Standardize Columns
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'vibe_tags'
    ) THEN
        ALTER TABLE vibe_tags RENAME TO tags;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'tags' AND column_name = 'vibe_tag_id'
    ) THEN
        ALTER TABLE tags RENAME COLUMN vibe_tag_id TO tag_id;
    END IF;
END $$;

-- Add tag_category_id and scale_value to TAGS
ALTER TABLE tags ADD COLUMN IF NOT EXISTS tag_category_id UUID;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS scale_value INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tags_category'
    ) THEN
        ALTER TABLE tags
            ADD CONSTRAINT fk_tags_category
            FOREIGN KEY (tag_category_id)
            REFERENCES tag_categories (tag_category_id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- Migrate category text data to tag_category_id before dropping redundant column
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'tags' AND column_name = 'category'
    ) THEN
        UPDATE tags
        SET tag_category_id = (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE')
        WHERE category = 'VIBE' AND tag_category_id IS NULL;

        UPDATE tags
        SET tag_category_id = (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE')
        WHERE category = 'PURPOSE' AND tag_category_id IS NULL;

        ALTER TABLE tags DROP COLUMN category;
    END IF;
END $$;

-- Seed 5 Standard Noise Tags with Scale Values (1 to 5)
INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
SELECT 'Yên tĩnh tuyệt đối', 'Không gian tĩnh lặng, phù hợp làm việc tập trung cao độ hoặc đọc sách', tag_category_id, 1, TRUE
FROM tag_categories WHERE code = 'NOISE'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
SELECT 'Khá yên tĩnh', 'Thì thầm, trò chuyện nhỏ nhẹ, âm thanh nền tối thiểu', tag_category_id, 2, TRUE
FROM tag_categories WHERE code = 'NOISE'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
SELECT 'Vừa phải', 'Âm thanh nền quán cà phê điển hình, nhạc nền dịu nhẹ', tag_category_id, 3, TRUE
FROM tag_categories WHERE code = 'NOISE'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
SELECT 'Sôi động', 'Nhạc rõ, đông đúc, thích hợp giao lưu tụ tập bạn bè', tag_category_id, 4, TRUE
FROM tag_categories WHERE code = 'NOISE'
ON CONFLICT (name) DO NOTHING;

INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
SELECT 'Náo nhiệt', 'Âm thanh lớn, acoustic sôi động hoặc phong cách pub/bar-cafe', tag_category_id, 5, TRUE
FROM tag_categories WHERE code = 'NOISE'
ON CONFLICT (name) DO NOTHING;

-- 3. Rename store_vibe_tags to STORE_TAGS and Standardize Columns
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'store_vibe_tags'
    ) THEN
        ALTER TABLE store_vibe_tags RENAME TO store_tags;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'store_tags' AND column_name = 'store_vibe_tag_id'
    ) THEN
        ALTER TABLE store_tags RENAME COLUMN store_vibe_tag_id TO store_tag_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'store_tags' AND column_name = 'vibe_tag_id'
    ) THEN
        ALTER TABLE store_tags RENAME COLUMN vibe_tag_id TO tag_id;
    END IF;
END $$;

-- Add proof_image_url & reject_reason to STORE_TAGS
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS proof_image_url TEXT;
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS reject_reason TEXT;

-- 4. Align Related Tables (vibe_survey_logs, tag_ratings)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'vibe_survey_logs' AND column_name = 'vibe_tag_id'
    ) THEN
        ALTER TABLE vibe_survey_logs RENAME COLUMN vibe_tag_id TO tag_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_name = 'tag_ratings' AND column_name = 'vibe_tag_id'
    ) THEN
        ALTER TABLE tag_ratings RENAME COLUMN vibe_tag_id TO tag_id;
    END IF;
END $$;

-- 5. Create ONBOARDING_QUESTIONS table
CREATE TABLE IF NOT EXISTS onboarding_questions (
    question_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_category_id UUID,
    title           VARCHAR(255) NOT NULL,
    subtitle        TEXT,
    question_type   VARCHAR(30)  NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 1,
    is_required     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    max_selections  INTEGER      DEFAULT 3,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_onboarding_questions_category
        FOREIGN KEY (tag_category_id)
        REFERENCES tag_categories (tag_category_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_onboarding_question_type
        CHECK (question_type IN ('SINGLE_SELECT', 'MULTI_SELECT', 'SLIDER'))
);

CREATE INDEX IF NOT EXISTS idx_onboarding_questions_active ON onboarding_questions(is_active);
CREATE INDEX IF NOT EXISTS idx_onboarding_questions_order ON onboarding_questions(display_order);

-- Seed Default Onboarding Questions
INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
VALUES
    (
        (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'),
        'Bạn tìm kiếm không gian như thế nào về độ yên tĩnh?',
        'Kéo thanh trượt 5 mức độ để chọn không gian phù hợp với bạn nhất',
        'SLIDER',
        1,
        TRUE,
        TRUE,
        1
    ),
    (
        (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'),
        'Mục đích chính của bạn khi đến quán cà phê là gì?',
        'Chọn tối đa 3 mục đích bạn thường hướng tới',
        'MULTI_SELECT',
        2,
        TRUE,
        TRUE,
        3
    ),
    (
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'Bạn yêu thích gu thiết kế & vibe không gian nào?',
        'Chọn tối đa 3 phong cách quán mang lại cảm hứng tốt nhất cho bạn',
        'MULTI_SELECT',
        3,
        TRUE,
        TRUE,
        3
    );

-- 6. Create USER_PREFERENCES table
CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    question_id   UUID NOT NULL,
    tag_id        UUID,
    numeric_value INTEGER,
    is_skipped    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_preferences_question
        FOREIGN KEY (question_id)
        REFERENCES onboarding_questions (question_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_preferences_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags (tag_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_preferences_question_id ON user_preferences(question_id);

-- Conditional Unique Indexes to prevent duplicate selections
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_pref_tag 
    ON user_preferences(user_id, question_id, tag_id) 
    WHERE tag_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_pref_slider 
    ON user_preferences(user_id, question_id) 
    WHERE numeric_value IS NOT NULL;

-- Drop legacy noise_tolerance column from users since noise preferences are now dynamically stored in user_preferences
ALTER TABLE users DROP COLUMN IF EXISTS noise_tolerance;
