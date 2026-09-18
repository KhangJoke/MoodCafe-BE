-- =============================================================
-- R__03: SEED ONBOARDING QUESTIONS (4 QUESTIONS FOR 4 CATEGORIES)
-- =============================================================

INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
SELECT 
    tc.tag_category_id,
    'Bạn tìm kiếm không gian như thế nào về độ yên tĩnh?',
    'Kéo thanh trượt 5 mức độ để chọn không gian phù hợp với bạn nhất',
    'SLIDER', 1, TRUE, TRUE, 1
FROM tag_categories tc WHERE tc.code = 'NOISE'
ON CONFLICT DO NOTHING;

INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
SELECT 
    tc.tag_category_id,
    'Mục đích chính bạn thường tìm đến quán cà phê?',
    'Chọn tối đa 3 lý do bạn ghé quán hôm nay',
    'MULTI_SELECT', 2, TRUE, TRUE, 3
FROM tag_categories tc WHERE tc.code = 'PURPOSE'
ON CONFLICT DO NOTHING;

INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
SELECT 
    tc.tag_category_id,
    'Vibe không gian & phong cách thiết kế bạn yêu thích?',
    'Chọn tối đa 3 phong cách kiến trúc chạm đến cảm xúc của bạn',
    'MULTI_SELECT', 3, TRUE, TRUE, 3
FROM tag_categories tc WHERE tc.code = 'VIBE'
ON CONFLICT DO NOTHING;

INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
SELECT 
    tc.tag_category_id,
    'Những tiện ích thiết yếu bạn quan tâm nhất khi chọn quán?',
    'Chọn tối đa 3 tiện ích bạn luôn tìm kiếm tại quán',
    'MULTI_SELECT', 4, TRUE, TRUE, 3
FROM tag_categories tc WHERE tc.code = 'AMENITY'
ON CONFLICT DO NOTHING;
