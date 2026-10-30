-- ========================================================================================
-- V55: GAMIFICATION SCHEMA - QUESTS, TITLES, PROGRESS, AND AUDIT LOGS
-- DDL Only: Creates tables, constraints, and indexes. Seed data is in db/seed/R__13.
-- ========================================================================================

-- 1. TITLES TABLE
CREATE TABLE IF NOT EXISTS titles (
    title_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE' CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD')),
    color_hex VARCHAR(10) NOT NULL DEFAULT '#CD7F32',
    icon_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_titles_code ON titles(code);
CREATE INDEX IF NOT EXISTS idx_titles_tier ON titles(tier);

-- 2. USER TITLES TABLE (Kho danh hiệu cá nhân)
CREATE TABLE IF NOT EXISTS user_titles (
    user_title_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title_id UUID NOT NULL REFERENCES titles(title_id) ON DELETE CASCADE,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_user_titles_user_title UNIQUE (user_id, title_id)
);

CREATE INDEX IF NOT EXISTS idx_user_titles_user_id ON user_titles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_titles_equipped ON user_titles(user_id, is_equipped);

-- 3. QUESTS TABLE
CREATE TABLE IF NOT EXISTS quests (
    quest_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    quest_group VARCHAR(30) NOT NULL CHECK (quest_group IN ('ONBOARDING', 'CIVIL', 'EXPLORE', 'COMMUNITY', 'VIP', 'DAILY')),
    quest_type VARCHAR(20) NOT NULL CHECK (quest_type IN ('DAILY', 'MONTHLY', 'ONE_TIME')),
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('FIRST_CHECKIN', 'ONTIME_STREAK', 'DISTINCT_MOOD_CHECKIN', 'DISTINCT_STORE_CHECKIN', 'VERIFIED_REVIEW', 'TOTAL_SPEND', 'DAILY_ACTION')),
    target_value BIGINT NOT NULL DEFAULT 1 CHECK (target_value > 0),
    reward_points BIGINT NOT NULL DEFAULT 0 CHECK (reward_points >= 0),
    reward_voucher_template_id UUID REFERENCES voucher_templates(template_id) ON DELETE SET NULL,
    reward_title_id UUID REFERENCES titles(title_id) ON DELETE SET NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quests_code ON quests(code);
CREATE INDEX IF NOT EXISTS idx_quests_group ON quests(quest_group);
CREATE INDEX IF NOT EXISTS idx_quests_type ON quests(quest_type);

-- 4. USER QUEST PROGRESS TABLE (Tiến độ thực hiện)
CREATE TABLE IF NOT EXISTS user_quest_progress (
    user_quest_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    quest_id UUID NOT NULL REFERENCES quests(quest_id) ON DELETE CASCADE,
    period_key VARCHAR(30) NOT NULL, -- 'LIFETIME' | 'YYYY-MM' | 'YYYY-MM-DD'
    current_value BIGINT NOT NULL DEFAULT 0 CHECK (current_value >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'CLAIMABLE', 'REWARDED')),
    claimable_at TIMESTAMPTZ,
    rewarded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_quest_progress_period UNIQUE (user_id, quest_id, period_key)
);

CREATE INDEX IF NOT EXISTS idx_uqp_user_period ON user_quest_progress(user_id, period_key);
CREATE INDEX IF NOT EXISTS idx_uqp_status ON user_quest_progress(status);

-- 5. QUEST AUDIT LOGS TABLE (Nhật ký truy vết & chống gian lận)
CREATE TABLE IF NOT EXISTS quest_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    quest_id UUID NOT NULL REFERENCES quests(quest_id) ON DELETE CASCADE,
    period_key VARCHAR(30) NOT NULL,
    source_ref_id VARCHAR(100) NOT NULL, -- booking_id or review_id
    store_id UUID,
    action_date DATE NOT NULL,
    value_added BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quest_audit_source UNIQUE (quest_id, user_id, source_ref_id)
);

CREATE INDEX IF NOT EXISTS idx_qal_antifarm ON quest_audit_logs(user_id, store_id, action_date);
