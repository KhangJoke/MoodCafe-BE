-- =============================================================
-- R__04: SEED 20 STORES AND PRIMARY CLOUDINARY IMAGES
-- Idempotent batch insertion using WHERE NOT EXISTS
-- =============================================================

-- 1. Insert 20 Stores (Status: ACTIVE)
INSERT INTO stores (
    name, description, address, latitude, longitude,
    opening_time, closing_time, price_from, price_to,
    phone, email, status
)
SELECT 
    v.name, v.description, v.address, v.latitude, v.longitude,
    v.opening_time, v.closing_time, v.price_from, v.price_to,
    v.phone, v.email, 'ACTIVE'
FROM (VALUES
    ('The Workshop Specialty Coffee', 'Không gian phong cách công nghiệp ấn tượng, bàn lớn chuyên biệt cho làm việc tập trung và cà phê pha thủ công đỉnh cao.', '27 Ngô Đức Kế, Bến Nghé, Quận 1', 10.7731, 106.7048, '07:30:00'::TIME, '22:00:00'::TIME, 50000::bigint, 95000::bigint, '02838246810', 'workshop@moodcafe.com'),
    ('Yên Cà Phê Mộc & Sách', 'Góc nhỏ tĩnh lặng ngập tràn sách hay, ánh sáng tự nhiên chan hòa cùng tiếng nhạc êm dịu giúp tái tạo năng lượng.', '15 Trần Quý Khoách, Tân Định, Quận 1', 10.7915, 106.6892, '07:00:00'::TIME, '22:30:00'::TIME, 35000::bigint, 60000::bigint, '02838246811', 'yen@moodcafe.com'),
    ('The Hideout Espresso & Lounge', 'Không gian sang trọng, ánh đèn vàng ấm cúng và sự riêng tư hoàn hảo cho những cuộc trò chuyện đôi lứa.', '42 Nguyễn Huệ, Bến Nghé, Quận 1', 10.7745, 106.7032, '08:00:00'::TIME, '23:00:00'::TIME, 55000::bigint, 90000::bigint, '02838246812', 'hideout@moodcafe.com'),
    ('The Green Haven Garden', 'Ốc đảo xanh mát với khu vườn nhiệt đới giữa lòng phố thị, thân thiện với thú cưng và cực kỳ thoáng đãng.', '19 Nguyễn Thị Diệu, Võ Thị Sáu, Quận 3', 10.7788, 106.6915, '06:30:00'::TIME, '22:00:00'::TIME, 45000::bigint, 80000::bigint, '02838246813', 'greenhaven@moodcafe.com'),
    ('Mây Concept Rooftop Cafe', 'Quán cà phê sân thượng lộng gió với tầm nhìn toàn cảnh hoàng hôn thành phố, góc chụp ảnh sống ảo tuyệt mỹ.', '36/2 Nguyễn Gia Trí, Phường 25, Bình Thạnh', 10.8035, 106.7152, '15:30:00'::TIME, '23:30:00'::TIME, 40000::bigint, 75000::bigint, '02838246814', 'mayrooftop@moodcafe.com'),
    ('Cỏ Mềm Garden & Tea', 'Nơi thưởng thức trà thảo mộc organic và bánh ngọt thủ công trong không gian chữa lành trầm lắng yên bình.', '82 Phan Xích Long, Phường 2, Phú Nhuận', 10.7968, 106.6908, '08:00:00'::TIME, '22:00:00'::TIME, 45000::bigint, 70000::bigint, '02838246815', 'comem@moodcafe.com'),
    ('Sống Vội Concept Workspace', 'Không gian làm việc sáng tạo trang bị bàn công thái học, wifi cáp quang riêng và ổ cắm tại từng ghế ngồi.', '142 Đinh Bộ Lĩnh, Phường 26, Bình Thạnh', 10.8123, 106.7105, '07:00:00'::TIME, '23:00:00'::TIME, 40000::bigint, 70000::bigint, '02838246816', 'songvoi@moodcafe.com'),
    ('Nhà Cổ 1985 Vintage Cafe', 'Căn biệt thự cổ Pháp với gạch hoa xưa cũ, chiếc tivi đen trắng và tách cà phê phin đậm đà phong vị Sài Gòn.', '212 Phan Đình Phùng, Phường 1, Phú Nhuận', 10.7932, 106.6841, '06:30:00'::TIME, '22:00:00'::TIME, 30000::bigint, 55000::bigint, '02838246817', 'nhaco1985@moodcafe.com'),
    ('Mood Cafe Thao Dien', 'Không gian nhiệt đới sang trọng chuẩn Âu tại khu phố Tây, bể cá Koi thư giãn và menu cocktail cà phê đặc sắc.', '18 Xuân Thủy, Thảo Điền, TP. Thủ Đức', 10.8042, 106.7328, '07:30:00'::TIME, '23:00:00'::TIME, 55000::bigint, 110000::bigint, '02838246818', 'thaodien@moodcafe.com'),
    ('The Oasis Botanical Cafe', 'Khu vườn nhà kính nhiệt đới ngập tràn ánh nắng và hàng trăm loại cây cảnh quý, cực kỳ thân thiện với cún cưng.', '12 Quốc Hương, Thảo Điền, TP. Thủ Đức', 10.8015, 106.7301, '07:00:00'::TIME, '22:00:00'::TIME, 50000::bigint, 85000::bigint, '02838246819', 'oasis@moodcafe.com'),
    ('Artisan Lab Specialty Coffee', 'Phòng thí nghiệm cà phê với máy rang xay hiện đại, không gian công nghiệp tinh gọn dành cho người yêu hương vị nguyên bản.', '45 Đường Số 7, An Phú, TP. Thủ Đức', 10.8021, 106.7455, '07:00:00'::TIME, '21:30:00'::TIME, 60000::bigint, 120000::bigint, '02838246820', 'artisanlab@moodcafe.com'),
    ('Sunset View River Cafe', 'View trực diện bờ sông Sài Gòn thơ mộng, không gian mở đón gió sông mát rượi ngắm tàu thuyền xuôi ngược.', '68 Nguyễn Văn Hưởng, Thảo Điền, TP. Thủ Đức', 10.8112, 106.7299, '07:00:00'::TIME, '22:30:00'::TIME, 55000::bigint, 95000::bigint, '02838246821', 'sunsetriver@moodcafe.com'),
    ('Chợ Lớn Heritage Coffee', 'Nét văn hóa Chợ Lớn giao thoa cổ kính, thưởng thức cà phê vợt và các loại trà hoa truyền thống giữa lòng phố Hoa.', '105 Triệu Quang Phục, Phường 11, Quận 5', 10.7548, 106.6622, '06:00:00'::TIME, '22:00:00'::TIME, 30000::bigint, 60000::bigint, '02838246822', 'cholonheritage@moodcafe.com'),
    ('Deadline Zone 24/7 Workspace', 'Thiên đường của sinh viên và cú đêm Sài Gòn, mở cửa 24/7 với hệ thống điều hòa liên tục và ghế đệm êm ái.', '284 Tô Hiến Thành, Phường 15, Quận 10', 10.7761, 106.6635, '00:00:00'::TIME, '23:59:00'::TIME, 35000::bigint, 65000::bigint, '02838246823', 'deadlinezone@moodcafe.com'),
    ('Vườn Nhiệt Đới Sài Gòn', 'Không gian rộng hơn 500m2 phủ bóng cây xanh, khu trò chơi giải trí boardgame vui nhộn dành cho nhóm đông.', '57 Thành Thái, Phường 14, Quận 10', 10.7712, 106.6588, '07:30:00'::TIME, '22:30:00'::TIME, 35000::bigint, 65000::bigint, '02838246824', 'vuonnhietdoi@moodcafe.com'),
    ('Hẻm Nhỏ Acoustic Coffee', 'Nằm sâu trong con hẻm thanh bình, đêm nhạc mộc guitar thứ Bảy hàng tuần mang lại những nốt nhạc lắng đọng.', '48 Trần Hưng Đạo, Phường 7, Quận 5', 10.7588, 106.6715, '08:00:00'::TIME, '23:00:00'::TIME, 40000::bigint, 75000::bigint, '02838246825', 'hemnhoacoustic@moodcafe.com'),
    ('Mood Cafe District 1', 'Trụ sở trung tâm thương hiệu MoodCafe, thiết kế hiện đại đa tiện ích với quầy bar Specialty và khu co-working.', '124 Lê Lợi, Bến Thành, Quận 1', 10.7728, 106.6989, '07:00:00'::TIME, '22:30:00'::TIME, 45000::bigint, 85000::bigint, '02838246826', 'district1@moodcafe.com'),
    ('Mood Cafe Phu Nhuan', 'Góc phố Hoa Mai rợp mát, không gian hoài niệm êm ả cùng hương cà phê thơm ngát cho những buổi hẹn đầm ấm.', '88 Hoa Mai, Phường 2, Phú Nhuận', 10.7954, 106.6923, '07:00:00'::TIME, '22:00:00'::TIME, 38000::bigint, 68000::bigint, '02838246827', 'phunhuan@moodcafe.com'),
    ('The Minimalist Studio & Cafe', 'Phong cách thiền Nhật Bản kết hợp studio ảnh tối giản, chất liệu gỗ sồi tự nhiên và trà matcha thượng hạng.', '25 Phổ Quang, Phường 2, Tân Bình', 10.8088, 106.6685, '08:00:00'::TIME, '21:30:00'::TIME, 50000::bigint, 85000::bigint, '02838246828', 'minimaliststudio@moodcafe.com'),
    ('Vòm Xanh Glasshouse Cafe', 'Nhà kính châu Âu với trần kính vòm cao đón trọn ánh sáng mặt trời, ngắm trọn khu vườn hoa lá xanh rì.', '102 Hồng Hà, Phường 2, Tân Bình', 10.8155, 106.6712, '07:00:00'::TIME, '22:30:00'::TIME, 45000::bigint, 80000::bigint, '02838246829', 'vomxanh@moodcafe.com')
) AS v(name, description, address, latitude, longitude, opening_time, closing_time, price_from, price_to, phone, email)
WHERE NOT EXISTS (
    SELECT 1 FROM stores s WHERE s.name = v.name AND s.is_deleted = FALSE
);

-- 2. Insert Primary Images for 20 Stores
INSERT INTO store_images (store_id, image_url, is_primary)
SELECT s.store_id, img.image_url, TRUE
FROM (VALUES
    ('The Workshop Specialty Coffee', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749503/moodcafe/stores/store_01_workshop.jpg'),
    ('Yên Cà Phê Mộc & Sách', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749505/moodcafe/stores/store_02_yen.jpg'),
    ('The Hideout Espresso & Lounge', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749506/moodcafe/stores/store_03_hideout.jpg'),
    ('The Green Haven Garden', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749507/moodcafe/stores/store_04_green_haven.jpg'),
    ('Mây Concept Rooftop Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749509/moodcafe/stores/store_05_may_rooftop.jpg'),
    ('Cỏ Mềm Garden & Tea', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749510/moodcafe/stores/store_06_co_mem.jpg'),
    ('Sống Vội Concept Workspace', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749511/moodcafe/stores/store_07_song_voi.jpg'),
    ('Nhà Cổ 1985 Vintage Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749512/moodcafe/stores/store_08_nha_co_1985.jpg'),
    ('Mood Cafe Thao Dien', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749514/moodcafe/stores/store_09_mood_thao_dien.jpg'),
    ('The Oasis Botanical Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749515/moodcafe/stores/store_10_oasis_botanical.jpg'),
    ('Artisan Lab Specialty Coffee', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749516/moodcafe/stores/store_11_artisan_lab.jpg'),
    ('Sunset View River Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749517/moodcafe/stores/store_12_sunset_river.jpg'),
    ('Chợ Lớn Heritage Coffee', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749519/moodcafe/stores/store_13_cho_lon_heritage.jpg'),
    ('Deadline Zone 24/7 Workspace', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749520/moodcafe/stores/store_14_deadline_zone.jpg'),
    ('Vườn Nhiệt Đới Sài Gòn', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749521/moodcafe/stores/store_15_vuon_nhiet_doi.jpg'),
    ('Hẻm Nhỏ Acoustic Coffee', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749522/moodcafe/stores/store_16_hem_nho_acoustic.jpg'),
    ('Mood Cafe District 1', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749523/moodcafe/stores/store_17_mood_district1.jpg'),
    ('Mood Cafe Phu Nhuan', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749525/moodcafe/stores/store_18_mood_phu_nhuan.jpg'),
    ('The Minimalist Studio & Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749526/moodcafe/stores/store_19_minimalist_studio.jpg'),
    ('Vòm Xanh Glasshouse Cafe', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749527/moodcafe/stores/store_20_vom_xanh_glasshouse.jpg')
) AS img(store_name, image_url)
JOIN stores s ON s.name = img.store_name AND s.is_deleted = FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM store_images si 
    WHERE si.store_id = s.store_id AND si.image_url = img.image_url AND si.is_deleted = FALSE
);
