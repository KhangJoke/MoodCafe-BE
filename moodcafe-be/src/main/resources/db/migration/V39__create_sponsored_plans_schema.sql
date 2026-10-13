-- =========================================================================
-- V39: SPONSORED PLANS SCHEMA & SEEDING DEFAULT PACKAGES
-- 1. Create sponsored_plans table for Admin to manage sponsored packages & pricing
-- 2. Add sponsored_plan_id to sponsored_listings table
-- 3. Seed default sponsored plans for all placements (7-day and 30-day packages)
-- 4. Backfill existing sponsored_listings with matching sponsored_plan_id
-- =========================================================================

CREATE TABLE IF NOT EXISTS sponsored_plans (
    sponsored_plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    placement VARCHAR(50) NOT NULL,
    duration_days INTEGER NOT NULL DEFAULT 7,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    description TEXT,
    badge_text VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sponsored_plan_placement
        CHECK (placement IN ('TOP_BANNER_VIP', 'NEW_OPENING', 'WEEKEND_PICKS', 'TRENDING_NEAR_YOU'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sponsored_plans_name
    ON sponsored_plans(name)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_sponsored_plans_placement ON sponsored_plans(placement);
CREATE INDEX IF NOT EXISTS idx_sponsored_plans_active ON sponsored_plans(is_active);

-- Alter sponsored_listings table
ALTER TABLE sponsored_listings
    ADD COLUMN IF NOT EXISTS sponsored_plan_id UUID REFERENCES sponsored_plans(sponsored_plan_id) ON DELETE SET NULL;

ALTER TABLE sponsored_listings
    ALTER COLUMN duration_type DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sponsored_listings_plan_id ON sponsored_listings(sponsored_plan_id);

-- Seed initial packages so Admin already has baseline plans to manage
INSERT INTO sponsored_plans (name, display_name, placement, duration_days, price, description, badge_text, is_active)
VALUES
    ('TOP_BANNER_VIP_WEEK', 'Top Banner VIP - 7 Ngày', 'TOP_BANNER_VIP', 7, 150000,
     'Hiển thị tại Carousel Banner VIP đầu trang chủ và đầu danh sách tìm kiếm trong 7 ngày', 'VIP', TRUE),
    ('TOP_BANNER_VIP_MONTH', 'Top Banner VIP - 30 Ngày', 'TOP_BANNER_VIP', 30, 480000,
     'Hiển thị tại Carousel Banner VIP đầu trang chủ và đầu danh sách tìm kiếm trong 30 ngày (Tiết kiệm 20%)', 'Khuyên dùng', TRUE),
    ('NEW_OPENING_WEEK', 'Quán Mới Nổi Bật - 7 Ngày', 'NEW_OPENING', 7, 100000,
     'Xuất hiện ưu tiên trong danh mục Quán mới khai trương và địa điểm đáng trải nghiệm trong 7 ngày', 'Mới', TRUE),
    ('NEW_OPENING_MONTH', 'Quán Mới Nổi Bật - 30 Ngày', 'NEW_OPENING', 30, 320000,
     'Xuất hiện ưu tiên trong danh mục Quán mới khai trương và địa điểm đáng trải nghiệm trong 30 ngày', 'Tiết kiệm 20%', TRUE),
    ('WEEKEND_PICKS_WEEK', 'Cuối Tuần Hot - 7 Ngày', 'WEEKEND_PICKS', 7, 120000,
     'Gợi ý hàng đầu trong danh mục Điểm đến Cuối tuần sôi động trong 7 ngày', 'Hot Cuối Tuần', TRUE),
    ('WEEKEND_PICKS_MONTH', 'Cuối Tuần Hot - 30 Ngày', 'WEEKEND_PICKS', 30, 384000,
     'Gợi ý hàng đầu trong danh mục Điểm đến Cuối tuần sôi động trong 30 ngày', 'Khuyên dùng', TRUE),
    ('TRENDING_NEAR_YOU_WEEK', 'Xu Hướng Gần Bạn - 7 Ngày', 'TRENDING_NEAR_YOU', 7, 90000,
     'Ưu tiên vị trí trong danh sách Xu hướng cho khách hàng truy cập tại cùng khu vực trong 7 ngày', 'Gần Bạn', TRUE),
    ('TRENDING_NEAR_YOU_MONTH', 'Xu Hướng Gần Bạn - 30 Ngày', 'TRENDING_NEAR_YOU', 30, 288000,
     'Ưu tiên vị trí trong danh sách Xu hướng cho khách hàng truy cập tại cùng khu vực trong 30 ngày', 'Tiết kiệm 20%', TRUE)
ON CONFLICT DO NOTHING;

-- Backfill existing sample listings to link to the corresponding plans
UPDATE sponsored_listings sl
SET sponsored_plan_id = sp.sponsored_plan_id
FROM sponsored_plans sp
WHERE sl.sponsored_plan_id IS NULL
  AND sl.placement = sp.placement
  AND (
    (sl.duration_type = 'ONE_WEEK' AND sp.duration_days = 7)
    OR (sl.duration_type = 'ONE_MONTH' AND sp.duration_days = 30)
  );
