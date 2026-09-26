-- =========================================================================
-- V31: SUBSCRIPTION PLANS, USER SUBSCRIPTIONS & SPONSORED LISTINGS SCHEMA
-- 1. Extend subscription_plans with feature matrix and seed 3 plans
-- 2. Create user_subscriptions table for merchant account tiers
-- 3. Create subscription_payments table for payment history
-- 4. Create sponsored_listings table for priority placement marketing
-- 5. Seed initial subscriptions and mock sponsored listings
-- =========================================================================

-- 1. Extend subscription_plans table
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS plan_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS max_branches INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS table_management BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS advanced_analytics BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deposit_rules BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allow_sponsored_listing BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS vip_hero_banner BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS qr_table_menu BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ai_recommendation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS monthly_free_sponsored_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dedicated_support BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_plans_plan_code
    ON subscription_plans(plan_code)
    WHERE is_deleted = FALSE;

-- Upsert / Seed 3 Plans: BASIC, PRO, PREMIUM
UPDATE subscription_plans SET
    plan_code = 'BASIC',
    display_name = 'Cơ Bản (Basic)',
    description = 'Dành cho quán nhỏ hoặc mới tham gia hệ thống MoodCafé. Đăng ký hiển thị 1 chi nhánh, nhận đặt bàn cơ bản và thống kê cơ bản.',
    price = 0,
    duration_days = 30,
    max_branches = 1,
    table_management = FALSE,
    advanced_analytics = FALSE,
    deposit_rules = FALSE,
    allow_sponsored_listing = FALSE,
    vip_hero_banner = FALSE,
    qr_table_menu = FALSE,
    ai_recommendation = FALSE,
    monthly_free_sponsored_count = 0,
    dedicated_support = FALSE
WHERE name = 'BASIC';

INSERT INTO subscription_plans (
    plan_code, name, display_name, description, price, duration_days,
    max_branches, table_management, advanced_analytics, deposit_rules,
    allow_sponsored_listing, vip_hero_banner, qr_table_menu, ai_recommendation,
    monthly_free_sponsored_count, dedicated_support, is_active, is_deleted
)
SELECT 'BASIC', 'BASIC', 'Cơ Bản (Basic)',
       'Dành cho quán nhỏ hoặc mới tham gia hệ thống MoodCafé. Đăng ký hiển thị 1 chi nhánh, nhận đặt bàn cơ bản và thống kê cơ bản.',
       0, 30, 1, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 0, FALSE, TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE name = 'BASIC');

UPDATE subscription_plans SET
    plan_code = 'PRO',
    display_name = 'Tăng Trưởng (PRO)',
    description = 'Dành cho chuỗi café vừa và nhỏ muốn tối ưu vận hành và tiếp cận khách hàng. Đăng ký tối đa 3 chi nhánh, quản lý sơ đồ bàn, báo cáo chuyên sâu và mở khóa Sponsored Listing.',
    price = 199000,
    duration_days = 30,
    max_branches = 3,
    table_management = TRUE,
    advanced_analytics = TRUE,
    deposit_rules = TRUE,
    allow_sponsored_listing = TRUE,
    vip_hero_banner = FALSE,
    qr_table_menu = FALSE,
    ai_recommendation = FALSE,
    monthly_free_sponsored_count = 0,
    dedicated_support = FALSE
WHERE name = 'PRO';

INSERT INTO subscription_plans (
    plan_code, name, display_name, description, price, duration_days,
    max_branches, table_management, advanced_analytics, deposit_rules,
    allow_sponsored_listing, vip_hero_banner, qr_table_menu, ai_recommendation,
    monthly_free_sponsored_count, dedicated_support, is_active, is_deleted
)
SELECT 'PRO', 'PRO', 'Tăng Trưởng (PRO)',
       'Dành cho chuỗi café vừa và nhỏ muốn tối ưu vận hành và tiếp cận khách hàng. Đăng ký tối đa 3 chi nhánh, quản lý sơ đồ bàn, báo cáo chuyên sâu và mở khóa Sponsored Listing.',
       199000, 30, 3, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 0, FALSE, TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE name = 'PRO');

UPDATE subscription_plans SET
    plan_code = 'PREMIUM',
    display_name = 'VIP Đặc Quyền (PREMIUM)',
    description = 'Định vị thương hiệu cao cấp, phủ sóng marketing và hỗ trợ kỹ thuật VIP 24/7. Không giới hạn chi nhánh, VIP Hero Banner, Menu QR từng bàn, AI gợi ý và tặng 01 lượt Sponsored Listing/tháng.',
    price = 499000,
    duration_days = 30,
    max_branches = -1,
    table_management = TRUE,
    advanced_analytics = TRUE,
    deposit_rules = TRUE,
    allow_sponsored_listing = TRUE,
    vip_hero_banner = TRUE,
    qr_table_menu = TRUE,
    ai_recommendation = TRUE,
    monthly_free_sponsored_count = 1,
    dedicated_support = TRUE
WHERE name = 'PREMIUM';

INSERT INTO subscription_plans (
    plan_code, name, display_name, description, price, duration_days,
    max_branches, table_management, advanced_analytics, deposit_rules,
    allow_sponsored_listing, vip_hero_banner, qr_table_menu, ai_recommendation,
    monthly_free_sponsored_count, dedicated_support, is_active, is_deleted
)
SELECT 'PREMIUM', 'PREMIUM', 'VIP Đặc Quyền (PREMIUM)',
       'Định vị thương hiệu cao cấp, phủ sóng marketing và hỗ trợ kỹ thuật VIP 24/7. Không giới hạn chi nhánh, VIP Hero Banner, Menu QR từng bàn, AI gợi ý và tặng 01 lượt Sponsored Listing/tháng.',
       499000, 30, -1, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 1, TRUE, TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE name = 'PREMIUM');


-- 2. Create user_subscriptions table
CREATE TABLE IF NOT EXISTS user_subscriptions (
    user_subscription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    subscription_plan_id UUID NOT NULL REFERENCES subscription_plans(subscription_plan_id),
    start_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    monthly_free_sponsored_used INTEGER NOT NULL DEFAULT 0,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_subscription_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status ON user_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_end_date ON user_subscriptions(end_date);


-- 3. Create subscription_payments table
CREATE TABLE IF NOT EXISTS subscription_payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user_subscription_id UUID REFERENCES user_subscriptions(user_subscription_id) ON DELETE SET NULL,
    subscription_plan_id UUID REFERENCES subscription_plans(subscription_plan_id) ON DELETE SET NULL,
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_url TEXT,
    paid_at TIMESTAMPTZ,
    notes TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sub_payment_status
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_sub_payments_user_id ON subscription_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_sub_payments_tx_code ON subscription_payments(transaction_code);
CREATE INDEX IF NOT EXISTS idx_sub_payments_status ON subscription_payments(status);


-- 4. Create sponsored_listings table
CREATE TABLE IF NOT EXISTS sponsored_listings (
    sponsored_listing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    placement VARCHAR(50) NOT NULL,
    duration_type VARCHAR(30) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_free_quota_used BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    view_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,
    custom_banner_url VARCHAR(500),
    title VARCHAR(255),
    transaction_code VARCHAR(100),
    payment_method VARCHAR(50),
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sponsored_placement
        CHECK (placement IN ('TOP_BANNER_VIP', 'NEW_OPENING', 'WEEKEND_PICKS', 'TRENDING_NEAR_YOU')),
    CONSTRAINT chk_sponsored_duration
        CHECK (duration_type IN ('ONE_WEEK', 'ONE_MONTH')),
    CONSTRAINT chk_sponsored_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_sponsored_store_id ON sponsored_listings(store_id);
CREATE INDEX IF NOT EXISTS idx_sponsored_user_id ON sponsored_listings(user_id);
CREATE INDEX IF NOT EXISTS idx_sponsored_placement ON sponsored_listings(placement);
CREATE INDEX IF NOT EXISTS idx_sponsored_status ON sponsored_listings(status);
CREATE INDEX IF NOT EXISTS idx_sponsored_dates ON sponsored_listings(start_date, end_date);


-- 5. Seed initial subscriptions and mock sponsored listings for testing & demonstration
-- Owner 1 (Stores 1-4) -> PREMIUM package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '20 days', 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner1@moodcafe.com' AND p.name = 'PREMIUM'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 2 (Stores 5-8) -> PRO package
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days', 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner2@moodcafe.com' AND p.name = 'PRO'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Owner 3 (Stores 9-12) -> BASIC package (Locked Sponsored Listing state)
INSERT INTO user_subscriptions (user_id, subscription_plan_id, start_date, end_date, status, monthly_free_sponsored_used)
SELECT u.user_id, p.subscription_plan_id, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL, 'ACTIVE', 0
FROM users u, subscription_plans p
WHERE u.email = 'owner3@moodcafe.com' AND p.name = 'BASIC'
  AND NOT EXISTS (SELECT 1 FROM user_subscriptions us WHERE us.user_id = u.user_id AND us.status = 'ACTIVE');

-- Seed Mock Sponsored Listings
INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    custom_banner_url, payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'TOP_BANNER_VIP', 'ONE_MONTH',
       CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days',
       480000, FALSE, 'ACTIVE', 1240, 185, 'Specialty Coffee Đích Thực Tại Trung Tâm',
       'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=1200&q=80',
       'VNPAY', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'The Workshop Specialty Coffee' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id);

INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'WEEKEND_PICKS', 'ONE_WEEK',
       CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '5 days',
       120000, FALSE, 'ACTIVE', 680, 92, 'Rooftop ngắm hoàng hôn cực chill cuối tuần',
       'MOMO', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'Mây Concept Rooftop Cafe' AND u.email = 'owner2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id);

INSERT INTO sponsored_listings (
    store_id, user_id, placement, duration_type, start_date, end_date,
    amount, is_free_quota_used, status, view_count, click_count, title,
    payment_method, payment_status
)
SELECT s.store_id, u.user_id, 'NEW_OPENING', 'ONE_WEEK',
       CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '6 days',
       100000, FALSE, 'ACTIVE', 430, 58, 'Không gian xanh ngập tràn ánh nắng',
       'VNPAY', 'SUCCESS'
FROM stores s, users u
WHERE s.name = 'The Green Haven Garden' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM sponsored_listings sl WHERE sl.store_id = s.store_id);
