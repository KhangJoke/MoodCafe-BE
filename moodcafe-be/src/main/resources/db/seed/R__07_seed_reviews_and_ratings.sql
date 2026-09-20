-- =============================================================
-- R__07: SEED 15 REVIEWS AND TAG RATINGS
-- =============================================================

-- Function / Statement to seed review + tag ratings cleanly
DO $$
DECLARE
    u1 UUID; u2 UUID; u3 UUID; u4 UUID; u5 UUID;
    s1 UUID; s2 UUID; s3 UUID; s4 UUID; s5 UUID;
    s6 UUID; s7 UUID; s8 UUID; s9 UUID; s10 UUID;
    s11 UUID; s12 UUID; s13 UUID; s14 UUID; s17 UUID;
    r_id UUID;
BEGIN
    SELECT user_id INTO u1 FROM users WHERE email = 'customer1@moodcafe.com';
    SELECT user_id INTO u2 FROM users WHERE email = 'customer2@moodcafe.com';
    SELECT user_id INTO u3 FROM users WHERE email = 'customer3@moodcafe.com';
    SELECT user_id INTO u4 FROM users WHERE email = 'customer4@moodcafe.com';
    SELECT user_id INTO u5 FROM users WHERE email = 'customer5@moodcafe.com';

    SELECT store_id INTO s1 FROM stores WHERE name = 'The Workshop Specialty Coffee';
    SELECT store_id INTO s2 FROM stores WHERE name = 'Yên Cà Phê Mộc & Sách';
    SELECT store_id INTO s3 FROM stores WHERE name = 'The Hideout Espresso & Lounge';
    SELECT store_id INTO s4 FROM stores WHERE name = 'The Green Haven Garden';
    SELECT store_id INTO s5 FROM stores WHERE name = 'Mây Concept Rooftop Cafe';
    SELECT store_id INTO s6 FROM stores WHERE name = 'Cỏ Mềm Garden & Tea';
    SELECT store_id INTO s7 FROM stores WHERE name = 'Sống Vội Concept Workspace';
    SELECT store_id INTO s8 FROM stores WHERE name = 'Nhà Cổ 1985 Vintage Cafe';
    SELECT store_id INTO s9 FROM stores WHERE name = 'Mood Cafe Thao Dien';
    SELECT store_id INTO s10 FROM stores WHERE name = 'The Oasis Botanical Cafe';
    SELECT store_id INTO s11 FROM stores WHERE name = 'Artisan Lab Specialty Coffee';
    SELECT store_id INTO s12 FROM stores WHERE name = 'Sunset View River Cafe';
    SELECT store_id INTO s13 FROM stores WHERE name = 'Chợ Lớn Heritage Coffee';
    SELECT store_id INTO s14 FROM stores WHERE name = 'Deadline Zone 24/7 Workspace';
    SELECT store_id INTO s17 FROM stores WHERE name = 'Mood Cafe District 1';

    -- Review 1: The Workshop by u1
    IF s1 IS NOT NULL AND u1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s1 AND user_id = u1 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s1, u1, 4.9, 5, 5, 5, 5, 'Quán cà phê chuẩn chỉnh nhất Sài Gòn để chạy deadline. Cà phê Pour-over quá đỉnh!')
        RETURNING review_id INTO r_id;

        INSERT INTO review_images (review_id, image_url) VALUES (r_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749528/moodcafe/reviews/review_01.jpg');

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Học bài / Chạy deadline', 'Yên tĩnh', 'Ổ cắm điện');
    END IF;

    -- Review 2: The Workshop by u2
    IF s1 IS NOT NULL AND u2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s1 AND user_id = u2 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s1, u2, 4.8, 5, 4, 5, 5, 'Không gian đậm chất công xưởng, wifi cực mạnh và bàn làm việc siêu rộng.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Công xưởng (Industrial)', 'Yên tĩnh');
    END IF;

    -- Review 3: Yên Cà Phê by u5
    IF s2 IS NOT NULL AND u5 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s2 AND user_id = u5 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s2, u5, 4.9, 5, 5, 4, 4, 'Đúng nghĩa Yên! Bước vào quán là thấy nhẹ nhõm, sách hay ngập tràn.')
        RETURNING review_id INTO r_id;

        INSERT INTO review_images (review_id, image_url) VALUES (r_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749530/moodcafe/reviews/review_02.jpg');

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Thư giãn / Đọc sách', 'Yên tĩnh', 'Cổ điển (Vintage / Retro)');
    END IF;

    -- Review 4: The Hideout by u3
    IF s3 IS NOT NULL AND u3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s3 AND user_id = u3 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s3, u3, 4.8, 4, 5, 5, 4, 'Hẹn hò ở đây thì quá tuyệt. Không gian ấm cúng, nhạc Jazz êm tai.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Hẹn hò lãng mạn', 'Hiện đại & Sang trọng (Modern Luxury)');
    END IF;

    -- Review 5: Green Haven by u4
    IF s4 IS NOT NULL AND u4 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s4 AND user_id = u4 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s4, u4, 4.8, 4, 5, 4, 4, 'Dẫn cún cưng đi cafe cuối tuần thì đây là điểm số 1. Sân vườn xanh mướt!')
        RETURNING review_id INTO r_id;

        INSERT INTO review_images (review_id, image_url) VALUES (r_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749531/moodcafe/reviews/review_03.jpg');

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Thân thiện thú cưng', 'Sân vườn nhiệt đới (Tropical Garden)');
    END IF;

    -- Review 6: Mây Rooftop by u4
    IF s5 IS NOT NULL AND u4 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s5 AND user_id = u4 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s5, u4, 4.7, 3, 5, 4, 3, 'Ngắm hoàng hôn Landmark 81 cực đỉnh! Chụp ảnh bao đẹp.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Chụp ảnh / Sống ảo', 'Tầng thượng thoáng đãng (Rooftop)');
    END IF;

    -- Review 7: Cỏ Mềm by u5
    IF s6 IS NOT NULL AND u5 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s6 AND user_id = u5 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s6, u5, 4.8, 5, 4, 5, 4, 'Bánh ngọt tự làm rất thơm ngon, trà hoa cúc thảo mộc ngọt dịu thanh mát.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Phục vụ bánh ngọt', 'Chữa lành & Trầm lắng (Healing)');
    END IF;

    -- Review 8: Sống Vội Workspace by u1
    IF s7 IS NOT NULL AND u1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s7 AND user_id = u1 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s7, u1, 4.8, 5, 5, 5, 5, 'Bàn lớn làm việc nhóm rất tiện lợi, ghế công thái học ngồi lâu không đau lưng.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Bàn lớn làm việc', 'Làm việc cá nhân dài giờ');
    END IF;

    -- Review 9: Nhà Cổ 1985 by u2
    IF s8 IS NOT NULL AND u2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s8 AND user_id = u2 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s8, u2, 4.7, 4, 4, 4, 3, 'Gợi lại bao ký ức tuổi thơ. Cà phê phin truyền thống béo ngậy.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Cổ điển (Vintage / Retro)', 'Nghe nhạc & Thư giãn');
    END IF;

    -- Review 10: Mood Thao Dien by u3
    IF s9 IS NOT NULL AND u3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s9 AND user_id = u3 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s9, u3, 4.9, 4, 5, 5, 5, 'Không gian đẳng cấp tại Thảo Điền. Bãi đậu ô tô rộng thênh thang.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Hiện đại & Sang trọng (Modern Luxury)', 'Bãi đỗ ô tô');
    END IF;

    -- Review 11: The Oasis by u4
    IF s10 IS NOT NULL AND u4 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s10 AND user_id = u4 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s10, u4, 4.8, 4, 5, 4, 4, 'Vườn cây nhiệt đới đẹp mê mẩn, không khí trong lành mát mẻ.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Sân vườn nhiệt đới (Tropical Garden)', 'Thân thiện thú cưng');
    END IF;

    -- Review 12: Artisan Lab by u1
    IF s11 IS NOT NULL AND u1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s11 AND user_id = u1 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s11, u1, 4.9, 5, 5, 5, 5, 'Gu cà phê Specialty cực chuẩn, nhân viên pha chế am hiểu và nhiệt tình.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Thưởng thức cà phê đặc sản', 'Công xưởng (Industrial)');
    END IF;

    -- Review 13: Sunset River by u3
    IF s12 IS NOT NULL AND u3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s12 AND user_id = u3 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s12, u3, 4.8, 4, 5, 5, 4, 'Buổi chiều hoàng hôn buông xuống sông Sài Gòn lãng mạn không từ nào tả xiết.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Hẹn hò lãng mạn', 'Lãng mạn & Thơ mộng');
    END IF;

    -- Review 14: Deadline Zone by u2
    IF s14 IS NOT NULL AND u2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s14 AND user_id = u2 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s14, u2, 4.8, 5, 5, 5, 5, 'Cứu cánh cho mùa đồ án! Mở cửa xuyên đêm, máy lạnh mát lạnh cả ngày lẫn đêm.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Mở cửa khuya', 'Học bài / Chạy deadline');
    END IF;

    -- Review 15: Mood District 1 by u1
    IF s17 IS NOT NULL AND u1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM reviews WHERE store_id = s17 AND user_id = u1 AND is_deleted = false) THEN
        INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content)
        VALUES (s17, u1, 4.8, 5, 5, 4, 5, 'Nằm ngay trung tâm Quận 1 nhưng bước vào bên trong rất yên tĩnh và chuyên nghiệp.')
        RETURNING review_id INTO r_id;

        INSERT INTO tag_ratings (review_id, tag_id, score)
        SELECT r_id, t.tag_id, 5 FROM tags t WHERE t.name IN ('Tối giản (Minimalism)', 'Yên tĩnh');
    END IF;
END $$;

-- Sync calculated avg_score and review_count into store_tags
UPDATE store_tags st
SET avg_score = sub.avg_score,
    review_count = sub.cnt
FROM (
    SELECT tr.tag_id, r.store_id, ROUND(AVG(tr.score)::numeric, 1) as avg_score, COUNT(tr.tag_rating_id) as cnt
    FROM tag_ratings tr
    JOIN reviews r ON tr.review_id = r.review_id
    WHERE r.is_deleted = false AND tr.is_deleted = false
    GROUP BY tr.tag_id, r.store_id
) sub
WHERE st.tag_id = sub.tag_id AND st.store_id = sub.store_id;
