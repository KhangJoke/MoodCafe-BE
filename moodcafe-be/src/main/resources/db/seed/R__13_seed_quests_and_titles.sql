-- ========================================================================================
-- R__13: SEED GAMIFICATION TITLES, QUEST REWARD VOUCHERS, AND SYSTEM QUESTS
-- Repeatable Migration: Updates titles, vouchers, and system quests idempotent
-- ========================================================================================

-- 1. VOUCHER TEMPLATES SEED FOR QUEST REWARDS
INSERT INTO voucher_templates (title, description, face_value_vnd, point_cost, is_active, display_order, min_gross_amount, valid_days)
SELECT v.title, v.description, v.face_value_vnd, v.point_cost, TRUE, v.display_order, 0, 60
FROM (VALUES
    ('Voucher Thưởng 15.000đ', 'Phần thưởng hoàn thành nhiệm vụ Khách hàng Gương mẫu / Cây bút Tích cực', 15000.00, 15000, 10),
    ('Voucher Thưởng 20.000đ', 'Phần thưởng hoàn thành nhiệm vụ Bước chân Khám phá / Bậc thầy Cảm xúc', 20000.00, 20000, 11),
    ('Voucher Thưởng 30.000đ', 'Phần thưởng hoàn thành cột mốc Cây viết Tinh tế / Tri kỷ MoodCafe', 30000.00, 30000, 12),
    ('Voucher Thưởng 50.000đ', 'Phần thưởng hoàn thành cột mốc Tiếng nói Cộng đồng / Thượng khách Hoàng gia', 50000.00, 50000, 13)
) AS v(title, description, face_value_vnd, point_cost, display_order)
WHERE NOT EXISTS (
    SELECT 1 FROM voucher_templates vt WHERE vt.title = v.title
);

-- 2. SEED 12 TITLES ACROSS BRONZE, SILVER, AND GOLD TIERS
INSERT INTO titles (code, name, description, tier, color_hex, icon_url)
VALUES
    -- BRONZE TIERS
    ('TITLE_NEWBIE_CHILL', '☕ Tân Binh Hệ Chill', 'Hoàn tất nhận bàn cho đơn đặt bàn đầu tiên trên ứng dụng', 'BRONZE', '#CD7F32', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/newbie_chill.png'),
    ('TITLE_FIRST_SNAP', '📸 Thích Sống Ảo', 'Đăng bài đánh giá xác thực đầu tiên có kèm ảnh chụp thực tế', 'BRONZE', '#CD7F32', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/first_snap.png'),
    -- SILVER TIERS
    ('TITLE_CIVIL_MASTER', '⏱️ Đại Sứ Đúng Giờ', 'Chuỗi 5 đơn liên tiếp đến nhận bàn đúng hạn trên toàn hệ thống', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/civil_master.png'),
    ('TITLE_MOOD_HUNTER', '🧭 Kẻ Săn Mood', 'Hoàn tất trải nghiệm tại các quán thuộc 3 chủ đề Mood khác nhau', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/mood_hunter.png'),
    ('TITLE_CAFE_CONNOISSEUR', '☕ Chuyên Gia Cà Phê', 'Khám phá và trải nghiệm tại 5 chi nhánh quán cà phê đối tác khác nhau', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/cafe_connoisseur.png'),
    ('TITLE_COMMUNITY_WRITER', '✍️ Cây Bút Tích Cực', 'Đạt mốc 5 bài đánh giá xác thực có kèm hình ảnh không gian quán thực tế', 'SILVER', '#C0C0C0', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/community_writer.png'),
    -- GOLD TIERS
    ('TITLE_PUNCTUAL_LEGEND', '⚡ Huyền Thoại Chuẩn Giờ', 'Đạt chuỗi kỷ lục 10 đơn liên tiếp đến nhận bàn đúng hạn tuyệt đối', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/punctual_legend.png'),
    ('TITLE_MOOD_MASTER', '🌈 Bậc Thầy Cảm Xúc', 'Chinh phục trải nghiệm tại đủ 5 chủ đề Tâm trạng (Mood) phong phú', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/mood_master.png'),
    ('TITLE_COMMUNITY_CRITIC', '👑 Nhà Phê Bình Có Gu', 'Đạt mốc 10 bài đánh giá xác thực có kèm ảnh và nhận xét sâu sắc', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/community_critic.png'),
    ('TITLE_VIBE_INFLUENCER', '✨ Tiếng Nói Trọng Lượng', 'Đạt mốc 20 bài đánh giá xác thực chất lượng cao trong cộng đồng', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/vibe_influencer.png'),
    ('TITLE_VIP_SOULMATE', '🌟 Tri Kỷ MoodCafe', 'Tổng hạn mức chi tiêu đồ uống hoàn tất đạt 2.000.000 VNĐ', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/vip_soulmate.png'),
    ('TITLE_TOP_SPENDER', '💎 Thượng Khách Hoàng Gia', 'Tổng hạn mức chi tiêu đồ uống tích lũy đạt 5.000.000 VNĐ', 'GOLD', '#FFD700', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/titles/top_spender.png')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    tier = EXCLUDED.tier,
    color_hex = EXCLUDED.color_hex,
    icon_url = EXCLUDED.icon_url,
    is_active = TRUE;

-- 3. SEED SYSTEM QUESTS (15 MILESTONES + 3 FIXED DAILY QUESTS = 18 QUESTS)

-- GROUP 1: ONBOARDING
-- 1. ONBOARD_FIRST_BOOKING (Thưởng Title Tân Binh Hệ Chill)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'ONBOARD_FIRST_BOOKING', 'Khởi đầu đúng Mood', 'Hoàn tất nhận bàn cho đơn đặt bàn đầu tiên trên ứng dụng', 'ONBOARDING', 'ONE_TIME', 'FIRST_CHECKIN', 1, 50, t.title_id, 1, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_NEWBIE_CHILL'
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- GROUP 2: CIVIL (Văn minh - Chuẩn giờ nhận bàn)
-- 2. CIVIL_STREAK_3 (Hằng tháng - Thưởng Voucher 15k)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, display_order, is_system, is_active)
SELECT 'CIVIL_STREAK_3', 'Khách hàng Gương mẫu', 'Đạt chuỗi 3 đơn liên tiếp trong tháng đến nhận bàn đúng hạn (Nếu trễ quá hạn sẽ quay về 0)', 'CIVIL', 'MONTHLY', 'ONTIME_STREAK', 3, 100, vt.template_id, 10, TRUE, TRUE
FROM voucher_templates vt WHERE vt.title = 'Voucher Thưởng 15.000đ' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 3. CIVIL_MASTER_5 (Một lần - Thưởng Title Đại Sứ Đúng Giờ)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'CIVIL_MASTER_5', 'Chuẩn chỉnh từng phút', 'Đạt chuỗi 5 đơn liên tiếp đến nhận bàn đúng hạn trên toàn hệ thống mà không bị trễ quá hạn', 'CIVIL', 'ONE_TIME', 'ONTIME_STREAK', 5, 200, t.title_id, 11, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_CIVIL_MASTER'
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 4. CIVIL_LEGEND_10 (Một lần - Thưởng Voucher 30k + Title Huyền Thoại Chuẩn Giờ)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'CIVIL_LEGEND_10', 'Bản lĩnh Đúng giờ', 'Đạt chuỗi 10 đơn liên tiếp đến nhận bàn đúng hạn tuyệt đối không một lần trễ quá hạn', 'CIVIL', 'ONE_TIME', 'ONTIME_STREAK', 10, 500, vt.template_id, t.title_id, 12, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 30.000đ' AND t.code = 'TITLE_PUNCTUAL_LEGEND' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- GROUP 3: EXPLORE (Khám phá Mood & Quán)
-- 5. EXPLORE_3_MOODS (Một lần - Thưởng Title Kẻ Săn Mood)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'EXPLORE_3_MOODS', 'Hành trình Cảm xúc', 'Hoàn tất check-in tại các quán thuộc 3 chủ đề Tâm trạng (Mood) khác nhau', 'EXPLORE', 'ONE_TIME', 'DISTINCT_MOOD_CHECKIN', 3, 150, t.title_id, 20, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_MOOD_HUNTER'
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 6. EXPLORE_5_MOODS (Một lần - Thưởng Voucher 20k + Title Bậc Thầy Cảm Xúc)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'EXPLORE_5_MOODS', 'Bậc thầy Cảm xúc Toàn năng', 'Khám phá và check-in đủ 5 chủ đề Tâm trạng (Mood) khác nhau trên ứng dụng', 'EXPLORE', 'ONE_TIME', 'DISTINCT_MOOD_CHECKIN', 5, 300, vt.template_id, t.title_id, 21, TRUE, TRUE
FROM voucher_templates vt, titles t
WHERE vt.title = 'Voucher Thưởng 20.000đ' AND t.code = 'TITLE_MOOD_MASTER' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 7. EXPLORE_3_STORES (Hằng tháng - Thưởng Voucher 20k)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, display_order, is_system, is_active)
SELECT 'EXPLORE_3_STORES', 'Bước chân Khám phá', 'Trong tháng có đơn check-in trải nghiệm tại 3 chi nhánh quán khác nhau', 'EXPLORE', 'MONTHLY', 'DISTINCT_STORE_CHECKIN', 3, 120, vt.template_id, 22, TRUE, TRUE
FROM voucher_templates vt WHERE vt.title = 'Voucher Thưởng 20.000đ' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 8. EXPLORE_5_STORES (Một lần - Thưởng Title Chuyên Gia Cà Phê)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'EXPLORE_5_STORES', 'Bản đồ Quán quen', 'Tích lũy trải nghiệm check-in tại 5 chi nhánh quán cà phê đối tác khác nhau', 'EXPLORE', 'ONE_TIME', 'DISTINCT_STORE_CHECKIN', 5, 250, t.title_id, 23, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_CAFE_CONNOISSEUR'
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- GROUP 4: COMMUNITY (Cộng đồng Review & Sống ảo)
-- 9. COMMUNITY_FIRST_SNAP (Một lần - Thưởng Title Thích Sống Ảo)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_FIRST_SNAP', 'Góc nhìn Chân thực', 'Đăng bài đánh giá xác thực đầu tiên có kèm ít nhất 1 ảnh chụp thực tế và nhận xét tối thiểu 30 ký tự', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 1, 50, t.title_id, 30, TRUE, TRUE
FROM titles t WHERE t.code = 'TITLE_FIRST_SNAP'
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 10. COMMUNITY_WRITER_5 (Một lần - Thưởng Voucher 15k + Title Cây Bút Tích Cực)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_WRITER_5', 'Cây bút Tích cực', 'Đạt mốc 5 bài đánh giá xác thực có kèm hình ảnh không gian quán thực tế', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 5, 150, vt.template_id, t.title_id, 31, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 15.000đ' AND t.code = 'TITLE_COMMUNITY_WRITER' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 11. COMMUNITY_CRITIC_10 (Một lần - Thưởng Voucher 30k + Title Nhà Phê Bình Có Gu)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_CRITIC_10', 'Cây viết Tinh tế', 'Đạt mốc 10 bài đánh giá xác thực có kèm ảnh chụp thực tế và nhận xét tối thiểu 50 ký tự tại các quán', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 10, 300, vt.template_id, t.title_id, 32, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 30.000đ' AND t.code = 'TITLE_COMMUNITY_CRITIC' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 12. COMMUNITY_INFLUENCER_20 (Một lần - Thưởng Voucher 50k + Title Tiếng Nói Trọng Lượng)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'COMMUNITY_INFLUENCER_20', 'Tiếng nói Cộng đồng', 'Đạt cột mốc xuất sắc với 20 bài đánh giá xác thực có kèm ảnh và nhận xét chi tiết', 'COMMUNITY', 'ONE_TIME', 'VERIFIED_REVIEW', 20, 600, vt.template_id, t.title_id, 33, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 50.000đ' AND t.code = 'TITLE_VIBE_INFLUENCER' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- GROUP 5: VIP (Chi tiêu & Gắn bó)
-- 13. VIP_BRONZE_SPEND (Một lần - Thưởng Voucher 15k)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, display_order, is_system, is_active)
SELECT 'VIP_BRONZE_SPEND', 'Hội viên Thân thiết', 'Tổng giá trị chi tiêu đồ uống tích lũy tại các quán đạt 500.000 VNĐ', 'VIP', 'ONE_TIME', 'TOTAL_SPEND', 500000, 150, vt.template_id, 40, TRUE, TRUE
FROM voucher_templates vt WHERE vt.title = 'Voucher Thưởng 15.000đ' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 14. VIP_SOULMATE (Một lần - Thưởng Voucher 30k + Title Tri Kỷ MoodCafe)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'VIP_SOULMATE', 'Tri kỷ MoodCafe', 'Tổng giá trị chi tiêu đồ uống tích lũy tại các quán đạt 2.000.000 VNĐ', 'VIP', 'ONE_TIME', 'TOTAL_SPEND', 2000000, 500, vt.template_id, t.title_id, 41, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 30.000đ' AND t.code = 'TITLE_VIP_SOULMATE' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 15. VIP_ROYAL_SPEND (Một lần - Thưởng Voucher 50k + Title Thượng Khách Hoàng Gia)
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, reward_voucher_template_id, reward_title_id, display_order, is_system, is_active)
SELECT 'VIP_ROYAL_SPEND', 'Thượng khách Hoàng gia', 'Tổng giá trị chi tiêu đồ uống tích lũy tại các quán đạt 5.000.000 VNĐ', 'VIP', 'ONE_TIME', 'TOTAL_SPEND', 5000000, 1200, vt.template_id, t.title_id, 42, TRUE, TRUE
FROM voucher_templates vt, titles t 
WHERE vt.title = 'Voucher Thưởng 50.000đ' AND t.code = 'TITLE_TOP_SPENDER' LIMIT 1
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    reward_voucher_template_id = EXCLUDED.reward_voucher_template_id,
    reward_title_id = EXCLUDED.reward_title_id,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- GROUP 6: DAILY (3 NHIỆM VỤ CỐ ĐỊNH - RESET HẰNG NGÀY LÚC 00:00 VN)
-- 16. FIXED DAILY QUEST 1: Điểm danh Đúng Giờ Trong Ngày
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, display_order, is_system, is_active)
VALUES ('DAILY_ONTIME_CHECKIN', 'Điểm danh Đúng hẹn hôm nay', 'Đến nhận bàn đúng giờ cho bất kỳ đơn đặt bàn nào trong ngày', 'DAILY', 'DAILY', 'FIRST_CHECKIN', 1, 20, 100, TRUE, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 17. FIXED DAILY QUEST 2: Đánh Giá Xác Thực Trong Ngày
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, display_order, is_system, is_active)
VALUES ('DAILY_VERIFIED_REVIEW', 'Khoảnh khắc Đáng nhớ hôm nay', 'Đăng 1 bài đánh giá xác thực có ảnh chụp thực tế tại quán trong ngày', 'DAILY', 'DAILY', 'VERIFIED_REVIEW', 1, 25, 101, TRUE, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

-- 18. FIXED DAILY QUEST 3: Thưởng thức Đồ uống Trong Ngày
INSERT INTO quests (code, name, description, quest_group, quest_type, rule_type, target_value, reward_points, display_order, is_system, is_active)
VALUES ('DAILY_COFFEE_SPEND', 'Thưởng thức Đồ uống hôm nay', 'Hoàn tất đơn đặt bàn với giá trị đồ uống từ 50.000 VNĐ trở lên trong ngày', 'DAILY', 'DAILY', 'TOTAL_SPEND', 50000, 30, 102, TRUE, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    target_value = EXCLUDED.target_value,
    reward_points = EXCLUDED.reward_points,
    display_order = EXCLUDED.display_order,
    is_system = TRUE,
    is_active = TRUE;

