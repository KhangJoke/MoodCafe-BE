-- V24: Add review tag ratings support, store tag score metrics, and seed additional store tags and reviews

-- 1. Ensure columns on store_tags for caching tag rating metrics
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS avg_score DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE store_tags ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;

-- 2. Ensure indexes on tag_ratings
CREATE INDEX IF NOT EXISTS idx_tag_ratings_review_id ON tag_ratings(review_id);
CREATE INDEX IF NOT EXISTS idx_tag_ratings_tag_id ON tag_ratings(tag_id);
CREATE INDEX IF NOT EXISTS idx_tag_ratings_is_deleted ON tag_ratings(is_deleted);

-- 3. Seed additional approved store tags for active stores

-- District 1: Add 'Làm việc nhóm', 'Không gian ngoài trời'
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%District 1%'
  AND t.name IN ('Làm việc nhóm', 'Không gian ngoài trời')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- Thao Dien: Add 'Cho phép thú cưng', 'Gặp gỡ bạn bè', 'Ổ cắm điện từng bàn'
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%Thao Dien%'
  AND t.name IN ('Cho phép thú cưng', 'Gặp gỡ bạn bè', 'Ổ cắm điện từng bàn')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- Phu Nhuan: Add 'Tối giản (Minimalism / Wabi-sabi)', 'Học bài / Chạy deadline', 'Ổ cắm điện từng bàn'
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%Phu Nhuan%'
  AND t.name IN ('Tối giản (Minimalism / Wabi-sabi)', 'Học bài / Chạy deadline', 'Ổ cắm điện từng bàn')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- 4. Seed initial tag ratings for seeded V19 reviews

-- District 1 Review 1 (staff1): Yên tĩnh (5), Học bài / Chạy deadline (5), Ổ cắm điện từng bàn (5), Wifi tốc độ cao (5)
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 5, CURRENT_TIMESTAMP - INTERVAL '2 days', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Yên tĩnh', 'Học bài / Chạy deadline', 'Ổ cắm điện từng bàn', 'Wifi tốc độ cao')
WHERE s.name LIKE '%District 1%' AND u.email = 'staff1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- Thao Dien Review 1 (staff2): Sân vườn nhiệt đới (5), Hẹn hò lãng mạn (5), Thư giãn / Đọc sách (4), Wifi tốc độ cao (5)
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 
    CASE WHEN t.name = 'Thư giãn / Đọc sách' THEN 4 ELSE 5 END,
    CURRENT_TIMESTAMP - INTERVAL '1 day', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Sân vườn nhiệt đới (Tropical Garden)', 'Hẹn hò lãng mạn', 'Thư giãn / Đọc sách', 'Wifi tốc độ cao')
WHERE s.name LIKE '%Thao Dien%' AND u.email = 'staff2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- Phu Nhuan Review 1 (owner1): Cổ điển (5), Đọc sách & Thư giãn (5), Khá yên tĩnh (4), Máy lạnh mát sâu (4)
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 
    CASE WHEN t.name IN ('Khá yên tĩnh', 'Máy lạnh mát sâu') THEN 4 ELSE 5 END,
    CURRENT_TIMESTAMP, FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Cổ điển (Vintage / Retro)', 'Đọc sách & Thư giãn', 'Khá yên tĩnh', 'Máy lạnh mát sâu')
WHERE s.name LIKE '%Phu Nhuan%' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- 5. Seed couple additional reviews with user reviews, photos, and tag ratings

-- District 1 Review 2 (owner2)
INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.9,
    5,
    5,
    5,
    5,
    'Không gian làm việc lý tưởng bậc nhất quận 1. Bàn ghế công thái học ngồi cả ngày không đau lưng, mạng wifi cực kỳ ổn định và máy lạnh luôn mát sâu.',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%District 1%' AND u.email = 'owner2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

-- District 1 Review 2 Tag Ratings
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 5, CURRENT_TIMESTAMP - INTERVAL '1 day', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Học bài / Chạy deadline', 'Yên tĩnh', 'Ổ cắm điện từng bàn', 'Wifi tốc độ cao', 'Máy lạnh mát sâu')
WHERE s.name LIKE '%District 1%' AND u.email = 'owner2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- District 1 Review 3 (admin)
INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.7,
    4,
    5,
    4,
    5,
    'Cà phê pha ngon, phong cách tối giản thanh lịch. Nhạc lofi nền êm dịu vừa phải giúp tăng độ tập trung cao độ.',
    CURRENT_TIMESTAMP - INTERVAL '6 hours',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%District 1%' AND u.email = 'admin@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

-- District 1 Review 3 Tag Ratings
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 
    CASE WHEN t.name = 'Yên tĩnh' THEN 4 ELSE 5 END,
    CURRENT_TIMESTAMP - INTERVAL '6 hours', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Tối giản (Minimalism / Wabi-sabi)', 'Yên tĩnh', 'Ổ cắm điện từng bàn')
WHERE s.name LIKE '%District 1%' AND u.email = 'admin@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- Thao Dien Review 2 (owner1)
INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.8,
    4,
    5,
    5,
    4,
    'Sân vườn nhiều cây xanh rất thoáng mát, dẫn cún cưng đi cà phê cuối tuần hết ý. Nước uống phong phú, nhân viên niềm nở.',
    CURRENT_TIMESTAMP - INTERVAL '12 hours',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%Thao Dien%' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

-- Thao Dien Review 2 Tag Ratings
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 
    CASE WHEN t.name = 'Wifi tốc độ cao' THEN 4 ELSE 5 END,
    CURRENT_TIMESTAMP - INTERVAL '12 hours', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Sân vườn nhiệt đới (Tropical Garden)', 'Cho phép thú cưng', 'Gặp gỡ bạn bè', 'Wifi tốc độ cao')
WHERE s.name LIKE '%Thao Dien%' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- Phu Nhuan Review 2 (staff2)
INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.7,
    4,
    4,
    5,
    5,
    'Góc ban công cổ kính ngắm đường phố rất thơ, kệ sách phong phú, trà sữa và pour-over rất ngon. Bàn ghế rộng rãi, ổ cắm đầy đủ.',
    CURRENT_TIMESTAMP - INTERVAL '18 hours',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%Phu Nhuan%' AND u.email = 'staff2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

-- Phu Nhuan Review 2 Tag Ratings
INSERT INTO tag_ratings (review_id, tag_id, score, created_at, is_deleted)
SELECT r.review_id, t.tag_id, 
    CASE WHEN t.name = 'Khá yên tĩnh' THEN 4 ELSE 5 END,
    CURRENT_TIMESTAMP - INTERVAL '18 hours', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
JOIN tags t ON t.name IN ('Cổ điển (Vintage / Retro)', 'Đọc sách & Thư giãn', 'Khá yên tĩnh', 'Ổ cắm điện từng bàn')
WHERE s.name LIKE '%Phu Nhuan%' AND u.email = 'staff2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM tag_ratings tr WHERE tr.review_id = r.review_id AND tr.tag_id = t.tag_id);

-- 6. Seed review images for newly created reviews
INSERT INTO review_images (review_id, image_url, is_deleted)
SELECT r.review_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643161/moodcafe/stores/store_district_1_primary.jpg', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
WHERE s.name LIKE '%District 1%' AND u.email = 'owner2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM review_images ri WHERE ri.review_id = r.review_id);

INSERT INTO review_images (review_id, image_url, is_deleted)
SELECT r.review_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643162/moodcafe/stores/store_thao_dien_primary.jpg', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
WHERE s.name LIKE '%Thao Dien%' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM review_images ri WHERE ri.review_id = r.review_id);

INSERT INTO review_images (review_id, image_url, is_deleted)
SELECT r.review_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643163/moodcafe/stores/store_phu_nhuan_primary.jpg', FALSE
FROM reviews r
JOIN stores s ON r.store_id = s.store_id
JOIN users u ON r.user_id = u.user_id
WHERE s.name LIKE '%Phu Nhuan%' AND u.email = 'staff2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM review_images ri WHERE ri.review_id = r.review_id);

-- 7. Backfill calculated avg_score and review_count into store_tags
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
