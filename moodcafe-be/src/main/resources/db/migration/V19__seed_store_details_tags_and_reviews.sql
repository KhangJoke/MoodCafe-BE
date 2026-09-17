-- V19: Seed Store Images, Approved Store Tags, Reviews, and Favorites for Active Stores

-- 1. SEED STORE IMAGES
INSERT INTO store_images (store_id, image_url, is_primary)
SELECT 
    s.store_id,
    CASE 
        WHEN s.name LIKE '%District 1%' THEN 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643161/moodcafe/stores/store_district_1_primary.jpg'
        WHEN s.name LIKE '%Thao Dien%' THEN 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643162/moodcafe/stores/store_thao_dien_primary.jpg'
        ELSE 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789643163/moodcafe/stores/store_phu_nhuan_primary.jpg'
    END,
    TRUE
FROM stores s
WHERE s.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM store_images si WHERE si.store_id = s.store_id);

-- 2. SEED APPROVED STORE TAGS
-- Quán 1 (District 1): Yên tĩnh, Học bài / Chạy deadline, Tối giản, Ổ cắm, Wifi, Máy lạnh
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%District 1%'
  AND t.name IN ('Tối giản (Minimalism / Wabi-sabi)', 'Học bài / Chạy deadline', 'Yên tĩnh', 'Ổ cắm điện từng bàn', 'Wifi tốc độ cao', 'Máy lạnh mát sâu')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- Quán 2 (Thao Dien): Sân vườn, Hẹn hò, Thư giãn, Bình thường, Wifi
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%Thao Dien%'
  AND t.name IN ('Sân vườn nhiệt đới (Tropical Garden)', 'Hẹn hò lãng mạn', 'Thư giãn / Đọc sách', 'Bình thường', 'Wifi tốc độ cao')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- Quán 3 (Phu Nhuan): Cổ điển, Đọc sách, Khá yên tĩnh, Máy lạnh, Wifi
INSERT INTO store_tags (store_id, tag_id, status)
SELECT s.store_id, t.tag_id, 'APPROVED'
FROM stores s, tags t
WHERE s.name LIKE '%Phu Nhuan%'
  AND t.name IN ('Cổ điển (Vintage / Retro)', 'Đọc sách & Thư giãn', 'Khá yên tĩnh', 'Máy lạnh mát sâu', 'Wifi tốc độ cao')
  AND NOT EXISTS (SELECT 1 FROM store_tags st WHERE st.store_id = s.store_id AND st.tag_id = t.tag_id);

-- 3. SEED INITIAL REVIEWS
INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.8,
    5,
    5,
    4,
    5,
    'Quán cực kỳ yên tĩnh, ổ cắm điện đầy đủ dưới gầm bàn, rất phù hợp cho dân IT và sinh viên ngồi chạy deadline.',
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%District 1%' AND u.email = 'staff1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.7,
    4,
    5,
    5,
    4,
    'Không gian sân vườn thoáng mát, nhiều góc sống ảo cực chill. Nhân viên thân thiện!',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%Thao Dien%' AND u.email = 'staff2@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

INSERT INTO reviews (store_id, user_id, overall_rating, quietness_rating, lighting_rating, seating_rating, outlet_rating, content, created_at, is_deleted)
SELECT 
    s.store_id,
    u.user_id,
    4.6,
    4,
    4,
    5,
    4,
    'Nhạc lofi nhẹ nhàng, kệ sách phong phú, trà sữa và pour-over rất ngon.',
    CURRENT_TIMESTAMP,
    FALSE
FROM stores s, users u
WHERE s.name LIKE '%Phu Nhuan%' AND u.email = 'owner1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.store_id = s.store_id AND r.user_id = u.user_id);

-- 4. SEED FAVORITE STORES
INSERT INTO favorite_stores (user_id, store_id, created_at)
SELECT u.user_id, s.store_id, CURRENT_TIMESTAMP
FROM users u, stores s
WHERE u.email = 'staff1@moodcafe.com'
  AND NOT EXISTS (SELECT 1 FROM favorite_stores fs WHERE fs.user_id = u.user_id AND fs.store_id = s.store_id);
