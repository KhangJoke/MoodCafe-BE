-- =============================================================
-- R__06: SEED USER ONBOARDING PREFERENCES (5 CUSTOMERS)
-- =============================================================

-- Customer 1 (Yên tĩnh, Học bài, Tối giản, Ổ cắm điện)
INSERT INTO user_preferences (user_id, question_id, tag_id)
SELECT u.user_id, q.question_id, t.tag_id
FROM users u, onboarding_questions q, tags t, tag_categories tc
WHERE u.email = 'customer1@moodcafe.com'
  AND q.tag_category_id = tc.tag_category_id
  AND (
    (tc.code = 'NOISE' AND t.name = 'Yên tĩnh') OR
    (tc.code = 'PURPOSE' AND t.name = 'Học bài / Chạy deadline') OR
    (tc.code = 'VIBE' AND t.name = 'Tối giản (Minimalism)') OR
    (tc.code = 'AMENITY' AND t.name = 'Ổ cắm điện')
  )
ON CONFLICT DO NOTHING;

-- Customer 2 (Yên tĩnh, Học bài, Cổ điển, Wifi)
INSERT INTO user_preferences (user_id, question_id, tag_id)
SELECT u.user_id, q.question_id, t.tag_id
FROM users u, onboarding_questions q, tags t, tag_categories tc
WHERE u.email = 'customer2@moodcafe.com'
  AND q.tag_category_id = tc.tag_category_id
  AND (
    (tc.code = 'NOISE' AND t.name = 'Yên tĩnh') OR
    (tc.code = 'PURPOSE' AND t.name = 'Học bài / Chạy deadline') OR
    (tc.code = 'VIBE' AND t.name = 'Cổ điển (Vintage / Retro)') OR
    (tc.code = 'AMENITY' AND t.name = 'Wifi')
  )
ON CONFLICT DO NOTHING;

-- Customer 3 (Khá yên tĩnh, Hẹn hò lãng mạn, Sân vườn nhiệt đới, Máy lạnh)
INSERT INTO user_preferences (user_id, question_id, tag_id)
SELECT u.user_id, q.question_id, t.tag_id
FROM users u, onboarding_questions q, tags t, tag_categories tc
WHERE u.email = 'customer3@moodcafe.com'
  AND q.tag_category_id = tc.tag_category_id
  AND (
    (tc.code = 'NOISE' AND t.name = 'Khá yên tĩnh') OR
    (tc.code = 'PURPOSE' AND t.name = 'Hẹn hò lãng mạn') OR
    (tc.code = 'VIBE' AND t.name = 'Sân vườn nhiệt đới (Tropical Garden)') OR
    (tc.code = 'AMENITY' AND t.name = 'Máy lạnh')
  )
ON CONFLICT DO NOTHING;

-- Customer 4 (Bình thường, Tụ tập bạn bè, Sân vườn nhiệt đới, Thân thiện thú cưng)
INSERT INTO user_preferences (user_id, question_id, tag_id)
SELECT u.user_id, q.question_id, t.tag_id
FROM users u, onboarding_questions q, tags t, tag_categories tc
WHERE u.email = 'customer4@moodcafe.com'
  AND q.tag_category_id = tc.tag_category_id
  AND (
    (tc.code = 'NOISE' AND t.name = 'Bình thường') OR
    (tc.code = 'PURPOSE' AND t.name = 'Tụ tập bạn bè') OR
    (tc.code = 'VIBE' AND t.name = 'Sân vườn nhiệt đới (Tropical Garden)') OR
    (tc.code = 'AMENITY' AND t.name = 'Thân thiện thú cưng')
  )
ON CONFLICT DO NOTHING;

-- Customer 5 (Yên tĩnh, Thư giãn / Đọc sách, Gỗ mộc ấm cúng, Nước lọc miễn phí)
INSERT INTO user_preferences (user_id, question_id, tag_id)
SELECT u.user_id, q.question_id, t.tag_id
FROM users u, onboarding_questions q, tags t, tag_categories tc
WHERE u.email = 'customer5@moodcafe.com'
  AND q.tag_category_id = tc.tag_category_id
  AND (
    (tc.code = 'NOISE' AND t.name = 'Yên tĩnh') OR
    (tc.code = 'PURPOSE' AND t.name = 'Thư giãn / Đọc sách') OR
    (tc.code = 'VIBE' AND t.name = 'Gỗ mộc ấm cúng (Rustic Wood)') OR
    (tc.code = 'AMENITY' AND t.name = 'Nước lọc miễn phí')
  )
ON CONFLICT DO NOTHING;
