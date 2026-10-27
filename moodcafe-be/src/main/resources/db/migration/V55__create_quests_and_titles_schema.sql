-- ========================================================================================
-- V55: GAMIFICATION SCHEMA - QUESTS, TITLES, PROGRESS, AND AUDIT LOGS
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

-- 3. VOUCHER TEMPLATES SEED FOR QUEST REWARDS
INSERT INTO voucher_templates (title, description, face_value_vnd, point_cost, is_active, display_order, min_gross_amount, valid_days)
SELECT v.title, v.description, v.face_value_vnd, v.point_cost, TRUE, v.display_order, 0, 60
FROM (VALUES
    ('Voucher Thưởng 15.000đ', 'Phần thưởng hoàn thành nhiệm vụ Khách hàng Gương mẫu', 15000.00, 15000, 10),
    ('Voucher Thưởng 30.000đ', 'Phần thưởng hoàn thành cột mốc Cây viết Tinh tế', 30000.00, 30000, 11)
) AS v(title, description, face_value_vnd, point_cost, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM voucher_templates vt WHERE vt.title = v.title
);

-- 4. SEED TITLES
INSERT INTO titles (code, name, description, tier, color_hex, icon_url)
VALUES
    ('TITLE_NEWBIE_CHILL', '☕ Tân Binh Hệ Chill', 'Hoàn tất nhận bàn cho đơn đặt bàn đầu tiên trên ứng dụng', 'BRONZE', '#CD7F32', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/newbie_chill.png'),
    ('TITLE_CIVIL_MASTER', '⏱️ Đại Sứ Đúng Giờ', 'Chuỗi 5 đơn liên tiếp đến nhận bàn đúng hạn trên toàn hệ thống', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/civil_master.png'),
    ('TITLE_MOOD_HUNTER', '🧭 Kẻ Săn Mood', 'Hoàn tất trải nghiệm tại các quán thuộc 3 chủ đề Mood khác nhau', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/mood_hunter.png'),
    ('TITLE_FIRST_SNAP', '📸 Thích Sống Ảo', 'Đăng bài đánh giá xác thực đầu tiên có kèm ảnh chụp thực tế', 'BRONZE', '#CD7F32', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/first_snap.png'),
    ('TITLE_COMMUNITY_CRITIC', '👑 Nhà Phê Bình Có Gu', 'Đạt mốc 10 bài đánh giá xác thực có kèm ảnh và nhận xét sâu sắc', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/community_critic.png'),
    ('TITLE_VIP_SOULMATE', '🌟 Tri Kỷ MoodCafe', 'Tổng hạn mức chi tiêu đồ uống hoàn tất đạt 2.000.000 VNĐ', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/vip_soulmate.png')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    tier = EXCLUDED.tier,
    color_hex = EXCLUDED.color_hex,
    icon_url = EXCLUDED.icon_url,
    is_active = TRUE;

-- 5. QUESTS TABLE
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

-- 6. USER QUEST PROGRESS TABLE (Tiến độ thực hiện)
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

-- 7. QUEST AUDIT LOGS TABLE (Nhật ký truy vết & chống gian lận)
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

-- 8. SEED 8 DEFAULT SYSTEM QUESTS + 2 SAMPLE DAILY QUESTS
-- 1. ONBOARD_FIRST_BOOKING
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'ONBOARD_FIRST_BOOKING', 'Khởi đầu đúng Mood', 'Hoàn tất nhận bàn cho đơn đặt bàn đầu tiên trên ứng dụng', 'ONBOARDING', 'ONE_TIME', 'FIRST_CHECKIN', 1, 50, t.title_id, 1, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_NEWBIE_CHILL'
ON CONFLICT (code) DO NOTHING;

-- 2. CIVIL_STREAK_3
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, display_order, is_system, is_active)
SELECT 'CIVIL_STREAK_3', 'Khách hàng Gương mẫu', 'Đạt chuỗi 3 đơn liên tiếp trong tháng đến nhận bàn đúng hạn (Nếu trễ quá hạn sẽ quay về 0)', 'CIVIL', 'MONTHLY', 'ONTIME_STREAK', 3, 100, vt.template_id, 2, TRUE, TRUE
FROM voucher_templates vt WHERE vt.title = 'Voucher Thưởng 15.000đ' LIMIT 1
ON CONFLICT (code) DO NOTHING;

-- 3. CIVIL_MASTER_5
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'CIVIL_MASTER_5', 'Chuẩn chỉnh từng phút', 'Đạt chuỗi 5 đơn liên tiếp đến nhận bàn đúng hạn trên toàn hệ thống mà không bị trễ quá hạn', 'CIVIL', 'ONE_TIME', 'ONTIME_STREAK', 5, 150, t.title_id, 3, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_CIVIL_MASTER'
ON CONFLICT (code) DO NOTHING;

-- 4. EXPLORE_3_MOODS
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'EXPLORE_3_MOODS', 'Bậc thầy Cảm xúc', 'Hoàn tất check-in tại các quán thuộc 3 chủ đề Tâm trạng (Mood) khác nhau', 'EXPLORE', 'ONE_TIME', 'DISTINCT_MOOD_CHECKIN', 3, 150, t.title_id, 4, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_MOOD_HUNTER'
ON CONFLICT (code) DO NOTHING;

-- 5. EXPLORE_3_STORES
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, display_order, is_system, is_active)
SELECT 'EXPLORE_3_STORES', 'Bước chân Khám phá', 'Trong tháng có đơn check-in trải nghiệm tại 3 chi nhánh quán khác nhau', 'EXPLORE', 'MONTHLY', 'DISTINCT_STORE_CHECKIN', 3, 120, vt.template_id, 5, TRUE, TRUE
FROM voucher_templates vt WHERE vt.face_value_vnd = 20000.00 LIMIT 1
ON CONFLICT (code) DO NOTHING;

-- 6. COMMUNITY_FIRST_SNAP
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_FIRST_SNAP', 'Góc nhìn Chân thực', 'Đăng bài đánh giá xác thực đầu tiên có kèm ít nhất 1 ảnh chụp thực tế và nhận xét tối thiểu 30 ký tự', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 1, 50, t.title_id, 6, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_FIRST_SNAP'
ON CONFLICT (code) DO NOTHING;

-- 7. COMMUNITY_CRITIC_10
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_CRITIC_10', 'Cây viết Tinh tế', 'Đạt mốc 10 bài đánh giá xác thực có kèm ảnh chụp thực tế và nhận xét tối thiểu 50 ký tự tại các quán', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 10, 300, vt.template_id, t.title_id, 7, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 30.000đ' AND t.code = 'TITLE_COMMUNITY_CRITIC' LIMIT 1
ON CONFLICT (code) DO NOTHING;

-- 8. VIP_SOULMATE
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'VIP_SOULMATE', 'Tri kỷ MoodCafe', 'Tổng hạn mức đồ uống gốc (gross_amount) của các đơn đã hoàn tất phục vụ đạt 2.000.000 VNĐ', 'VIP', 'ONE_TIME', 'TOTAL_SPEND', 2000000, 500, vt.template_id, t.title_id, 8, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.face_value_vnd = 50000.00 AND t.code = 'TITLE_VIP_SOULMATE' LIMIT 1
ON CONFLICT (code) DO NOTHING;

-- 9. SAMPLE DAILY QUEST 1: Check-in Đúng Giờ Trong Ngày
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, display_order, is_system, is_active)
VALUES ('DAILY_ONTIME_CHECKIN', 'Điểm danh Đúng hẹn hôm nay', 'Đến nhận bàn đúng giờ cho bất kỳ đơn đặt bàn nào trong ngày', 'DAILY', 'DAILY', 'FIRST_CHECKIN', 1, 20, 100, FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 10. SAMPLE DAILY QUEST 2: Đánh Giá Trong Ngày
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, display_order, is_system, is_active)
VALUES ('DAILY_VERIFIED_REVIEW', 'Khoảnh khắc Đáng nhớ hôm nay', 'Đăng 1 bài đánh giá xác thực có ảnh chụp thực tế tại quán trong ngày', 'DAILY', 'DAILY', 'VERIFIED_REVIEW', 1, 25, 101, FALSE, TRUE)
ON CONFLICT (code) DO NOTHING;
