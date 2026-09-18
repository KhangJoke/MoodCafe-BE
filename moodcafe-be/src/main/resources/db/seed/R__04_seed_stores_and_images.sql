-- =============================================================
-- R__04: SEED 20 STORES AND PRIMARY CLOUDINARY IMAGES
-- =============================================================

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'The Workshop Specialty Coffee',
    'Không gian phong cách công nghiệp ấn tượng, bàn lớn chuyên biệt cho làm việc tập trung và cà phê pha thủ công đỉnh cao.',
    '27 Ngô Đức Kế, Bến Nghé, Quận 1',
    'Quận 1',
    10.7731, 106.7048,
    '07:30:00'::TIME, '22:00:00'::TIME,
    '50.000 - 95.000 VND', 50000, 95000,
    '02838246810', 'workshop@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749503/moodcafe/stores/store_01_workshop.jpg', TRUE
FROM stores s WHERE s.name = 'The Workshop Specialty Coffee'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Yên Cà Phê Mộc & Sách',
    'Góc nhỏ tĩnh lặng ngập tràn sách hay, ánh sáng tự nhiên chan hòa cùng tiếng nhạc êm dịu giúp tái tạo năng lượng.',
    '15 Trần Quý Khoách, Tân Định, Quận 1',
    'Quận 1',
    10.7915, 106.6892,
    '07:00:00'::TIME, '22:30:00'::TIME,
    '35.000 - 60.000 VND', 35000, 60000,
    '02838246811', 'yen@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749505/moodcafe/stores/store_02_yen.jpg', TRUE
FROM stores s WHERE s.name = 'Yên Cà Phê Mộc & Sách'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'The Hideout Espresso & Lounge',
    'Không gian sang trọng, ánh đèn vàng ấm cúng và sự riêng tư hoàn hảo cho những cuộc trò chuyện đôi lứa.',
    '42 Nguyễn Huệ, Bến Nghé, Quận 1',
    'Quận 1',
    10.7745, 106.7032,
    '08:00:00'::TIME, '23:00:00'::TIME,
    '55.000 - 90.000 VND', 55000, 90000,
    '02838246812', 'hideout@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749506/moodcafe/stores/store_03_hideout.jpg', TRUE
FROM stores s WHERE s.name = 'The Hideout Espresso & Lounge'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'The Green Haven Garden',
    'Ốc đảo xanh mát với khu vườn nhiệt đới giữa lòng phố thị, thân thiện với thú cưng và cực kỳ thoáng đãng.',
    '19 Nguyễn Thị Diệu, Võ Thị Sáu, Quận 3',
    'Quận 3',
    10.7788, 106.6915,
    '06:30:00'::TIME, '22:00:00'::TIME,
    '45.000 - 80.000 VND', 45000, 80000,
    '02838246813', 'greenhaven@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749507/moodcafe/stores/store_04_green_haven.jpg', TRUE
FROM stores s WHERE s.name = 'The Green Haven Garden'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Mây Concept Rooftop Cafe',
    'Quán cà phê sân thượng lộng gió với tầm nhìn toàn cảnh hoàng hôn thành phố, góc chụp ảnh sống ảo tuyệt mỹ.',
    '36/2 Nguyễn Gia Trí, Phường 25, Bình Thạnh',
    'Bình Thạnh',
    10.8035, 106.7152,
    '15:30:00'::TIME, '23:30:00'::TIME,
    '40.000 - 75.000 VND', 40000, 75000,
    '02838246814', 'mayrooftop@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749509/moodcafe/stores/store_05_may_rooftop.jpg', TRUE
FROM stores s WHERE s.name = 'Mây Concept Rooftop Cafe'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Cỏ Mềm Garden & Tea',
    'Nơi thưởng thức trà thảo mộc organic và bánh ngọt thủ công trong không gian chữa lành trầm lắng yên bình.',
    '82 Phan Xích Long, Phường 2, Phú Nhuận',
    'Phú Nhuận',
    10.7968, 106.6908,
    '08:00:00'::TIME, '22:00:00'::TIME,
    '45.000 - 70.000 VND', 45000, 70000,
    '02838246815', 'comem@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749510/moodcafe/stores/store_06_co_mem.jpg', TRUE
FROM stores s WHERE s.name = 'Cỏ Mềm Garden & Tea'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Sống Vội Concept Workspace',
    'Không gian làm việc sáng tạo trang bị bàn công thái học, wifi cáp quang riêng và ổ cắm tại từng ghế ngồi.',
    '142 Đinh Bộ Lĩnh, Phường 26, Bình Thạnh',
    'Bình Thạnh',
    10.8123, 106.7105,
    '07:00:00'::TIME, '23:00:00'::TIME,
    '40.000 - 70.000 VND', 40000, 70000,
    '02838246816', 'songvoi@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749511/moodcafe/stores/store_07_song_voi.jpg', TRUE
FROM stores s WHERE s.name = 'Sống Vội Concept Workspace'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Nhà Cổ 1985 Vintage Cafe',
    'Căn biệt thự cổ Pháp với gạch hoa xưa cũ, chiếc tivi đen trắng và tách cà phê phin đậm đà phong vị Sài Gòn.',
    '212 Phan Đình Phùng, Phường 1, Phú Nhuận',
    'Phú Nhuận',
    10.7932, 106.6841,
    '06:30:00'::TIME, '22:00:00'::TIME,
    '30.000 - 55.000 VND', 30000, 55000,
    '02838246817', 'nhaco1985@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749512/moodcafe/stores/store_08_nha_co_1985.jpg', TRUE
FROM stores s WHERE s.name = 'Nhà Cổ 1985 Vintage Cafe'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Mood Cafe Thao Dien',
    'Không gian nhiệt đới sang trọng chuẩn Âu tại khu phố Tây, bể cá Koi thư giãn và menu cocktail cà phê đặc sắc.',
    '18 Xuân Thủy, Thảo Điền, TP. Thủ Đức',
    'TP. Thủ Đức',
    10.8042, 106.7328,
    '07:30:00'::TIME, '23:00:00'::TIME,
    '55.000 - 110.000 VND', 55000, 110000,
    '02838246818', 'thaodien@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749514/moodcafe/stores/store_09_mood_thao_dien.jpg', TRUE
FROM stores s WHERE s.name = 'Mood Cafe Thao Dien'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'The Oasis Botanical Cafe',
    'Khu vườn nhà kính nhiệt đới ngập tràn ánh nắng và hàng trăm loại cây cảnh quý, cực kỳ thân thiện với cún cưng.',
    '12 Quốc Hương, Thảo Điền, TP. Thủ Đức',
    'TP. Thủ Đức',
    10.8015, 106.7301,
    '07:00:00'::TIME, '22:00:00'::TIME,
    '50.000 - 85.000 VND', 50000, 85000,
    '02838246819', 'oasis@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749515/moodcafe/stores/store_10_oasis_botanical.jpg', TRUE
FROM stores s WHERE s.name = 'The Oasis Botanical Cafe'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Artisan Lab Specialty Coffee',
    'Phòng thí nghiệm cà phê với máy rang xay hiện đại, không gian công nghiệp tinh gọn dành cho người yêu hương vị nguyên bản.',
    '45 Đường Số 7, An Phú, TP. Thủ Đức',
    'TP. Thủ Đức',
    10.8021, 106.7455,
    '07:00:00'::TIME, '21:30:00'::TIME,
    '60.000 - 120.000 VND', 60000, 120000,
    '02838246820', 'artisanlab@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749516/moodcafe/stores/store_11_artisan_lab.jpg', TRUE
FROM stores s WHERE s.name = 'Artisan Lab Specialty Coffee'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Sunset View River Cafe',
    'View trực diện bờ sông Sài Gòn thơ mộng, không gian mở đón gió sông mát rượi ngắm tàu thuyền xuôi ngược.',
    '68 Nguyễn Văn Hưởng, Thảo Điền, TP. Thủ Đức',
    'TP. Thủ Đức',
    10.8112, 106.7299,
    '07:00:00'::TIME, '22:30:00'::TIME,
    '55.000 - 95.000 VND', 55000, 95000,
    '02838246821', 'sunsetriver@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749517/moodcafe/stores/store_12_sunset_river.jpg', TRUE
FROM stores s WHERE s.name = 'Sunset View River Cafe'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Chợ Lớn Heritage Coffee',
    'Nét văn hóa Chợ Lớn giao thoa cổ kính, thưởng thức cà phê vợt và các loại trà hoa truyền thống giữa lòng phố Hoa.',
    '105 Triệu Quang Phục, Phường 11, Quận 5',
    'Quận 5',
    10.7548, 106.6622,
    '06:00:00'::TIME, '22:00:00'::TIME,
    '30.000 - 60.000 VND', 30000, 60000,
    '02838246822', 'cholonheritage@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749519/moodcafe/stores/store_13_cho_lon_heritage.jpg', TRUE
FROM stores s WHERE s.name = 'Chợ Lớn Heritage Coffee'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Deadline Zone 24/7 Workspace',
    'Thiên đường của sinh viên và cú đêm Sài Gòn, mở cửa 24/7 với hệ thống điều hòa liên tục và ghế đệm êm ái.',
    '284 Tô Hiến Thành, Phường 15, Quận 10',
    'Quận 10',
    10.7761, 106.6635,
    '00:00:00'::TIME, '23:59:00'::TIME,
    '35.000 - 65.000 VND', 35000, 65000,
    '02838246823', 'deadlinezone@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749520/moodcafe/stores/store_14_deadline_zone.jpg', TRUE
FROM stores s WHERE s.name = 'Deadline Zone 24/7 Workspace'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Vườn Nhiệt Đới Sài Gòn',
    'Không gian rộng hơn 500m2 phủ bóng cây xanh, khu trò chơi giải trí boardgame vui nhộn dành cho nhóm đông.',
    '57 Thành Thái, Phường 14, Quận 10',
    'Quận 10',
    10.7712, 106.6588,
    '07:30:00'::TIME, '22:30:00'::TIME,
    '35.000 - 65.000 VND', 35000, 65000,
    '02838246824', 'vuonnhietdoi@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749521/moodcafe/stores/store_15_vuon_nhiet_doi.jpg', TRUE
FROM stores s WHERE s.name = 'Vườn Nhiệt Đới Sài Gòn'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Hẻm Nhỏ Acoustic Coffee',
    'Nằm sâu trong con hẻm thanh bình, đêm nhạc mộc guitar thứ Bảy hàng tuần mang lại những nốt nhạc lắng đọng.',
    '48 Trần Hưng Đạo, Phường 7, Quận 5',
    'Quận 5',
    10.7588, 106.6715,
    '08:00:00'::TIME, '23:00:00'::TIME,
    '40.000 - 75.000 VND', 40000, 75000,
    '02838246825', 'hemnhoacoustic@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749522/moodcafe/stores/store_16_hem_nho_acoustic.jpg', TRUE
FROM stores s WHERE s.name = 'Hẻm Nhỏ Acoustic Coffee'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Mood Cafe District 1',
    'Trụ sở trung tâm thương hiệu MoodCafe, thiết kế hiện đại đa tiện ích với quầy bar Specialty và khu co-working.',
    '124 Lê Lợi, Bến Thành, Quận 1',
    'Quận 1',
    10.7728, 106.6989,
    '07:00:00'::TIME, '22:30:00'::TIME,
    '45.000 - 85.000 VND', 45000, 85000,
    '02838246826', 'district1@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749523/moodcafe/stores/store_17_mood_district1.jpg', TRUE
FROM stores s WHERE s.name = 'Mood Cafe District 1'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Mood Cafe Phu Nhuan',
    'Góc phố Hoa Mai rợp mát, không gian hoài niệm êm ả cùng hương cà phê thơm ngát cho những buổi hẹn đầm ấm.',
    '88 Hoa Mai, Phường 2, Phú Nhuận',
    'Phú Nhuận',
    10.7954, 106.6923,
    '07:00:00'::TIME, '22:00:00'::TIME,
    '38.000 - 68.000 VND', 38000, 68000,
    '02838246827', 'phunhuan@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749525/moodcafe/stores/store_18_mood_phu_nhuan.jpg', TRUE
FROM stores s WHERE s.name = 'Mood Cafe Phu Nhuan'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'The Minimalist Studio & Cafe',
    'Phong cách thiền Nhật Bản kết hợp studio ảnh tối giản, chất liệu gỗ sồi tự nhiên và trà matcha thượng hạng.',
    '25 Phổ Quang, Phường 2, Tân Bình',
    'Tân Bình',
    10.8088, 106.6685,
    '08:00:00'::TIME, '21:30:00'::TIME,
    '50.000 - 85.000 VND', 50000, 85000,
    '02838246828', 'minimaliststudio@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749526/moodcafe/stores/store_19_minimalist_studio.jpg', TRUE
FROM stores s WHERE s.name = 'The Minimalist Studio & Cafe'
ON CONFLICT DO NOTHING;

INSERT INTO stores (
    name, description, address, district, latitude, longitude,
    opening_time, closing_time, price_range, price_from, price_to,
    phone, email, status
)
VALUES (
    'Vòm Xanh Glasshouse Cafe',
    'Nhà kính châu Âu với trần kính vòm cao đón trọn ánh sáng mặt trời, ngắm trọn khu vườn hoa lá xanh rì.',
    '102 Hồng Hà, Phường 2, Tân Bình',
    'Tân Bình',
    10.8155, 106.6712,
    '07:00:00'::TIME, '22:30:00'::TIME,
    '45.000 - 80.000 VND', 45000, 80000,
    '02838246829', 'vomxanh@moodcafe.com',
    'ACTIVE'
)
ON CONFLICT DO NOTHING;

INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749527/moodcafe/stores/store_20_vom_xanh_glasshouse.jpg', TRUE
FROM stores s WHERE s.name = 'Vòm Xanh Glasshouse Cafe'
ON CONFLICT DO NOTHING;
