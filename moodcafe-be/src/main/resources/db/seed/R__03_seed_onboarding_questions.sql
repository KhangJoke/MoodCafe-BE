-- =============================================================
-- R__03: SEED ONBOARDING QUESTIONS (4 QUESTIONS FOR 4 CATEGORIES)
-- Completely idempotent using WHERE NOT EXISTS per category
-- =============================================================

INSERT INTO onboarding_questions (tag_category_id, title, subtitle, question_type, display_order, is_required, is_active, max_selections)
SELECT 
    tc.tag_category_id, q.title, q.subtitle, q.question_type, q.display_order, TRUE, TRUE, q.max_selections
FROM (VALUES
    ('NOISE', 'Bạn tìm kiếm không gian như thế nào về độ yên tĩnh?', 'Kéo thanh trượt 5 mức độ để chọn không gian phù hợp với bạn nhất', 'SLIDER', 1, 1),
    ('PURPOSE', 'Mục đích chính bạn thường tìm đến quán cà phê?', 'Chọn tối đa 3 lý do bạn ghé quán hôm nay', 'MULTI_SELECT', 2, 3),
    ('VIBE', 'Vibe không gian & phong cách thiết kế bạn yêu thích?', 'Chọn tối đa 3 phong cách kiến trúc chạm đến cảm xúc của bạn', 'MULTI_SELECT', 3, 3),
    ('AMENITY', 'Những tiện ích thiết yếu bạn quan tâm nhất khi chọn quán?', 'Chọn tối đa 3 tiện ích bạn luôn tìm kiếm tại quán', 'MULTI_SELECT', 4, 3)
) AS q(category_code, title, subtitle, question_type, display_order, max_selections)
JOIN tag_categories tc ON tc.code = q.category_code AND tc.is_deleted = FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM onboarding_questions oq
    WHERE oq.tag_category_id = tc.tag_category_id AND oq.is_deleted = FALSE
);
