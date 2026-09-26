-- =============================================================
-- R__05: SEED STORE TAGS (EXACTLY 4 HIGHLIGHTED TAGS PER STORE)
-- =============================================================

-- Tags for: The Workshop Specialty Coffee
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Workshop Specialty Coffee' AND t.name = 'Học bài / Chạy deadline'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Workshop Specialty Coffee' AND t.name = 'Công xưởng (Industrial)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Workshop Specialty Coffee' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Workshop Specialty Coffee' AND t.name = 'Ổ cắm điện'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'The Workshop Specialty Coffee' AND t.name = 'Wifi'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Yên Cà Phê Mộc & Sách
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Yên Cà Phê Mộc & Sách' AND t.name = 'Thư giãn / Đọc sách'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Yên Cà Phê Mộc & Sách' AND t.name = 'Cổ điển (Vintage / Retro)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Yên Cà Phê Mộc & Sách' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Yên Cà Phê Mộc & Sách' AND t.name = 'Nước lọc miễn phí'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Yên Cà Phê Mộc & Sách' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: The Hideout Espresso & Lounge
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Hideout Espresso & Lounge' AND t.name = 'Hẹn hò lãng mạn'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Hideout Espresso & Lounge' AND t.name = 'Hiện đại & Sang trọng (Modern Luxury)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Hideout Espresso & Lounge' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Hideout Espresso & Lounge' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'The Hideout Espresso & Lounge' AND t.name = 'Thanh toán thẻ/QR'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: The Green Haven Garden
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Green Haven Garden' AND t.name = 'Tụ tập bạn bè'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Green Haven Garden' AND t.name = 'Sân vườn nhiệt đới (Tropical Garden)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Green Haven Garden' AND t.name = 'Bình thường'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Green Haven Garden' AND t.name = 'Thân thiện thú cưng'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'The Green Haven Garden' AND t.name = 'Khu vực ngoài trời'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Mây Concept Rooftop Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mây Concept Rooftop Cafe' AND t.name = 'Chụp ảnh / Sống ảo'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mây Concept Rooftop Cafe' AND t.name = 'Tầng thượng thoáng đãng (Rooftop)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mây Concept Rooftop Cafe' AND t.name = 'Khá sôi động'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mây Concept Rooftop Cafe' AND t.name = 'Khu vực ngoài trời'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Mây Concept Rooftop Cafe' AND t.name = 'Wifi'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Cỏ Mềm Garden & Tea
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Cỏ Mềm Garden & Tea' AND t.name = 'Ăn nhẹ & Uống trà'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Cỏ Mềm Garden & Tea' AND t.name = 'Chữa lành & Trầm lắng (Healing)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Cỏ Mềm Garden & Tea' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Cỏ Mềm Garden & Tea' AND t.name = 'Phục vụ bánh ngọt'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Cỏ Mềm Garden & Tea' AND t.name = 'Ghế êm ái'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Sống Vội Concept Workspace
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sống Vội Concept Workspace' AND t.name = 'Làm việc cá nhân dài giờ'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sống Vội Concept Workspace' AND t.name = 'Tối giản (Minimalism)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sống Vội Concept Workspace' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sống Vội Concept Workspace' AND t.name = 'Bàn lớn làm việc'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Sống Vội Concept Workspace' AND t.name = 'Wifi'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Nhà Cổ 1985 Vintage Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe' AND t.name = 'Nghe nhạc & Thư giãn'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe' AND t.name = 'Cổ điển (Vintage / Retro)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe' AND t.name = 'Không hút thuốc'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe' AND t.name = 'Nước lọc miễn phí'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Mood Cafe Thao Dien
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Thao Dien' AND t.name = 'Gặp gỡ đối tác / Bán hàng'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Thao Dien' AND t.name = 'Hiện đại & Sang trọng (Modern Luxury)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Thao Dien' AND t.name = 'Bình thường'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Thao Dien' AND t.name = 'Bãi đỗ ô tô'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Thao Dien' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: The Oasis Botanical Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Oasis Botanical Cafe' AND t.name = 'Thư giãn / Đọc sách'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Oasis Botanical Cafe' AND t.name = 'Sân vườn nhiệt đới (Tropical Garden)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Oasis Botanical Cafe' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Oasis Botanical Cafe' AND t.name = 'Thân thiện thú cưng'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'The Oasis Botanical Cafe' AND t.name = 'Khu vực ngoài trời'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Artisan Lab Specialty Coffee
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Artisan Lab Specialty Coffee' AND t.name = 'Thưởng thức cà phê đặc sản'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Artisan Lab Specialty Coffee' AND t.name = 'Công xưởng (Industrial)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Artisan Lab Specialty Coffee' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Artisan Lab Specialty Coffee' AND t.name = 'Ổ cắm điện'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Artisan Lab Specialty Coffee' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Sunset View River Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sunset View River Cafe' AND t.name = 'Hẹn hò lãng mạn'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sunset View River Cafe' AND t.name = 'Lãng mạn & Thơ mộng'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sunset View River Cafe' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Sunset View River Cafe' AND t.name = 'Khu vực ngoài trời'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Sunset View River Cafe' AND t.name = 'Thanh toán thẻ/QR'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Chợ Lớn Heritage Coffee
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Chợ Lớn Heritage Coffee' AND t.name = 'Trò chuyện riêng tư'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Chợ Lớn Heritage Coffee' AND t.name = 'Đông Dương (Indochine)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Chợ Lớn Heritage Coffee' AND t.name = 'Bình thường'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Chợ Lớn Heritage Coffee' AND t.name = 'Bãi đỗ xe máy'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Chợ Lớn Heritage Coffee' AND t.name = 'Nước lọc miễn phí'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Deadline Zone 24/7 Workspace
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Học bài / Chạy deadline'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Tối giản (Minimalism)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Mở cửa khuya'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Wifi'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Deadline Zone 24/7 Workspace' AND t.name = 'Ổ cắm điện'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Vườn Nhiệt Đới Sài Gòn
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn' AND t.name = 'Chơi Boardgame & Giải trí'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn' AND t.name = 'Năng động & Trẻ trung'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn' AND t.name = 'Khá sôi động'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn' AND t.name = 'Bãi đỗ xe máy'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn' AND t.name = 'Khu vực ngoài trời'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Hẻm Nhỏ Acoustic Coffee
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee' AND t.name = 'Tâm sự sâu lắng'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee' AND t.name = 'Gỗ mộc ấm cúng (Rustic Wood)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee' AND t.name = 'Ghế êm ái'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee' AND t.name = 'Không hút thuốc'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Mood Cafe District 1
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe District 1' AND t.name = 'Học bài / Chạy deadline'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe District 1' AND t.name = 'Tối giản (Minimalism)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe District 1' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe District 1' AND t.name = 'Ổ cắm điện'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe District 1' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Mood Cafe Phu Nhuan
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Phu Nhuan' AND t.name = 'Thư giãn / Đọc sách'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Phu Nhuan' AND t.name = 'Cổ điển (Vintage / Retro)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Phu Nhuan' AND t.name = 'Khá yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Phu Nhuan' AND t.name = 'Máy lạnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Mood Cafe Phu Nhuan' AND t.name = 'Nước lọc miễn phí'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: The Minimalist Studio & Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Minimalist Studio & Cafe' AND t.name = 'Chụp ảnh / Sống ảo'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Minimalist Studio & Cafe' AND t.name = 'Phong cách Nhật Bản (Japanese / Zen)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Minimalist Studio & Cafe' AND t.name = 'Yên tĩnh'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'The Minimalist Studio & Cafe' AND t.name = 'Không hút thuốc'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'The Minimalist Studio & Cafe' AND t.name = 'Thanh toán thẻ/QR'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;

-- Tags for: Vòm Xanh Glasshouse Cafe
INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vòm Xanh Glasshouse Cafe' AND t.name = 'Hẹn hò lãng mạn'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vòm Xanh Glasshouse Cafe' AND t.name = 'Hàn Quốc (Korean Aesthetic)'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vòm Xanh Glasshouse Cafe' AND t.name = 'Bình thường'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', TRUE
FROM stores s, tags t
WHERE s.name = 'Vòm Xanh Glasshouse Cafe' AND t.name = 'Phục vụ bánh ngọt'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = TRUE;

INSERT INTO store_tags (store_id, tag_id, status, is_highlighted)
SELECT s.store_id, t.tag_id, 'APPROVED', FALSE
FROM stores s, tags t
WHERE s.name = 'Vòm Xanh Glasshouse Cafe' AND t.name = 'Bãi đỗ xe máy'
ON CONFLICT (store_id, tag_id) WHERE is_deleted = FALSE DO UPDATE 
SET status = 'APPROVED', is_highlighted = FALSE;
