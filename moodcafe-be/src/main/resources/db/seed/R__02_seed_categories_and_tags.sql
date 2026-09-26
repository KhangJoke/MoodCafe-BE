-- =============================================================
-- R__02: SEED 4 TAG CATEGORIES AND 50 DISTINCT TAGS WITH CLOUDINARY ASSETS
-- =============================================================

-- 1. Insert 4 Categories
INSERT INTO tag_categories (code, name, approval_mode, control_type, display_order, is_active)
VALUES 
    ('NOISE', 'Độ yên tĩnh & Âm thanh', 'OWNER_CUSTOM', 'SLIDER', 1, TRUE),
    ('PURPOSE', 'Mục đích sử dụng', 'OWNER_REQUEST', 'TAG_LIST', 2, TRUE),
    ('VIBE', 'Vibe & Phong cách', 'OWNER_REQUEST', 'TAG_LIST', 3, TRUE),
    ('AMENITY', 'Tiện ích quán', 'OWNER_REQUEST', 'TAG_LIST', 4, TRUE)
ON CONFLICT (code) WHERE is_deleted = FALSE DO UPDATE 
SET name = EXCLUDED.name, 
    approval_mode = EXCLUDED.approval_mode,
    control_type = EXCLUDED.control_type,
    display_order = EXCLUDED.display_order, 
    is_active = TRUE;

-- 2. Insert 5 NOISE Tags
INSERT INTO tags (tag_category_id, name, description, image_url, scale_value, is_active)
SELECT tc.tag_category_id, t.name, t.description, t.image_url, t.scale_value, TRUE
FROM tag_categories tc
CROSS JOIN (VALUES
    ('Yên tĩnh', 'Không gian tĩnh lặng, thích hợp tập trung cao độ', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749496/moodcafe/tags/noise_quiet.jpg', 1),
    ('Khá yên tĩnh', 'Âm thanh nền vừa phải, tiếng nhạc dịu êm', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749498/moodcafe/tags/noise_moderately_quiet.jpg', 2),
    ('Bình thường', 'Mức độ âm thanh phổ thông, trò chuyện tự nhiên', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749499/moodcafe/tags/noise_normal.jpg', 3),
    ('Khá sôi động', 'Nhạc có nhịp điệu, không khí náo nhiệt vừa phải', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749501/moodcafe/tags/noise_moderately_lively.jpg', 4),
    ('Sôi động', 'Âm nhạc năng động, tụ tập rôm rả, năng lượng cao', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749502/moodcafe/tags/noise_lively.jpg', 5)
) AS t(name, description, image_url, scale_value)
WHERE tc.code = 'NOISE'
ON CONFLICT (name) WHERE is_deleted = FALSE DO UPDATE 
SET image_url = EXCLUDED.image_url, description = EXCLUDED.description, scale_value = EXCLUDED.scale_value, is_active = TRUE;

-- 3. Insert 15 VIBE Tags
INSERT INTO tags (tag_category_id, name, description, image_url, is_active)
SELECT tc.tag_category_id, t.name, t.description, t.image_url, TRUE
FROM tag_categories tc
CROSS JOIN (VALUES
    ('Tối giản (Minimalism)', 'Thiết kế tinh gọn, thoáng đãng, sắc màu trang nhã', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749432/moodcafe/tags/vibe_minimalism.jpg'),
    ('Cổ điển (Vintage / Retro)', 'Phong cách hoài niệm, đồ gỗ xưa và màu thời gian', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749434/moodcafe/tags/vibe_vintage.jpg'),
    ('Sân vườn nhiệt đới (Tropical Garden)', 'Cây xanh tươi mát, hòa mình với thiên nhiên tươi mát', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749435/moodcafe/tags/vibe_tropical_garden.jpg'),
    ('Công xưởng (Industrial)', 'Gạch mộc, bê tông, kim loại mạnh mẽ và cá tính', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749437/moodcafe/tags/vibe_industrial.jpg'),
    ('Hiện đại & Sang trọng (Modern Luxury)', 'Ánh sáng tinh tế, nội thất cao cấp và thanh lịch', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749438/moodcafe/tags/vibe_modern_luxury.jpg'),
    ('Phong cách Nhật Bản (Japanese / Zen)', 'Gỗ sáng, cửa lùa, không gian thiền tịnh tĩnh tại', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749439/moodcafe/tags/vibe_japanese_zen.jpg'),
    ('Hàn Quốc (Korean Aesthetic)', 'Tone màu pastel, ánh sáng ngập tràn, chụp ảnh cực xinh', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749441/moodcafe/tags/vibe_korean_aesthetic.jpg'),
    ('Đông Dương (Indochine)', 'Nét giao thoa văn hóa Pháp - Á Đông quý phái', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749442/moodcafe/tags/vibe_indochine.jpg'),
    ('Gỗ mộc ấm cúng (Rustic Wood)', 'Nhiều nội thất gỗ, ánh đèn vàng ấm áp thân tình', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749443/moodcafe/tags/vibe_rustic_wood.jpg'),
    ('Tầng thượng thoáng đãng (Rooftop)', 'Gió trời lồng lộng, ngắm hoàng hôn và phố xá trên cao', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749445/moodcafe/tags/vibe_rooftop.jpg'),
    ('Nghệ thuật & Sáng tạo (Art Studio)', 'Trưng bày tranh ảnh, đồ thủ công, truyền cảm hứng', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749446/moodcafe/tags/vibe_art_studio.jpg'),
    ('Chill Lofi & Nhẹ nhàng', 'Nhạc êm ái, ánh sáng mờ nhẹ, thư thái tâm hồn', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749447/moodcafe/tags/vibe_chill_lofi.jpg'),
    ('Chữa lành & Trầm lắng (Healing)', 'Không gian tĩnh tâm, tái tạo năng lượng tích cực', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749449/moodcafe/tags/vibe_healing.jpg'),
    ('Lãng mạn & Thơ mộng', 'Bàn nến, hoa tươi, góc riêng tư cho các cặp đôi', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749450/moodcafe/tags/vibe_romantic.jpg'),
    ('Năng động & Trẻ trung', 'Màu sắc tươi vui, nhịp sống hiện đại, tràn đầy sức sống', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749451/moodcafe/tags/vibe_energetic.jpg')
) AS t(name, description, image_url)
WHERE tc.code = 'VIBE'
ON CONFLICT (name) WHERE is_deleted = FALSE DO UPDATE 
SET image_url = EXCLUDED.image_url, description = EXCLUDED.description, is_active = TRUE;

-- 4. Insert 15 PURPOSE Tags
INSERT INTO tags (tag_category_id, name, description, image_url, is_active)
SELECT tc.tag_category_id, t.name, t.description, t.image_url, TRUE
FROM tag_categories tc
CROSS JOIN (VALUES
    ('Học bài / Chạy deadline', 'Tập trung học tập và làm việc năng suất cao', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749452/moodcafe/tags/purpose_deadline.jpg'),
    ('Làm việc nhóm / Họp', 'Không gian bàn lớn thuận tiện thảo luận công việc', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749454/moodcafe/tags/purpose_teamwork.jpg'),
    ('Hẹn hò lãng mạn', 'Không gian ngọt ngào, ấm cúng cho buổi gặp gỡ lứa đôi', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749455/moodcafe/tags/purpose_dating.jpg'),
    ('Tụ tập bạn bè', 'Nơi thoải mái trò chuyện, cười đùa cùng hội bạn thân', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749456/moodcafe/tags/purpose_friends.jpg'),
    ('Thư giãn / Đọc sách', 'Góc ngồi yên tĩnh nhâm nhi cà phê và đọc cuốn sách hay', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749457/moodcafe/tags/purpose_reading.jpg'),
    ('Chụp ảnh / Sống ảo', 'Nhiều góc check-in lung linh lên hình triệu like', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749459/moodcafe/tags/purpose_photography.jpg'),
    ('Gặp gỡ đối tác / Bán hàng', 'Không gian lịch sự, chuyên nghiệp để bàn thảo hợp đồng', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749464/moodcafe/tags/purpose_business.jpg'),
    ('Chơi Boardgame & Giải trí', 'Giải trí thú vị cùng các tựa game bàn cờ hấp dẫn', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749466/moodcafe/tags/purpose_boardgame.jpg'),
    ('Thưởng thức cà phê đặc sản', 'Trải nghiệm hương vị hạt cà phê thượng hạng pha thủ công', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749468/moodcafe/tags/purpose_specialty_coffee.jpg'),
    ('Trò chuyện riêng tư', 'Bàn cách xa nhau, đảm bảo tính riêng tư cho câu chuyện', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749470/moodcafe/tags/purpose_private_chat.jpg'),
    ('Nghe nhạc & Thư giãn', 'Hòa mình vào playlist âm nhạc chọn lọc đầy cảm xúc', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749471/moodcafe/tags/purpose_music_relax.jpg'),
    ('Ngắm đường phố', 'Chỗ ngồi nhìn ra phố xá ngắm dòng người qua lại', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749472/moodcafe/tags/purpose_street_view.jpg'),
    ('Làm việc cá nhân dài giờ', 'Ghế êm, ổ điện, wifi ổn định ngồi cả ngày thoải mái', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749474/moodcafe/tags/purpose_deep_focus.jpg'),
    ('Tâm sự sâu lắng', 'Không gian kín đáo, nhẹ nhàng để chia sẻ buồn vui', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749475/moodcafe/tags/purpose_heartfelt_talk.jpg'),
    ('Ăn nhẹ & Uống trà', 'Menu đồ uống thanh vị kèm bánh ngọt thơm ngon', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749476/moodcafe/tags/purpose_tea_pastry.jpg')
) AS t(name, description, image_url)
WHERE tc.code = 'PURPOSE'
ON CONFLICT (name) WHERE is_deleted = FALSE DO UPDATE 
SET image_url = EXCLUDED.image_url, description = EXCLUDED.description, is_active = TRUE;

-- 5. Insert 15 AMENITY Tags (Ngắn gọn, chung chung)
INSERT INTO tags (tag_category_id, name, description, image_url, is_active)
SELECT tc.tag_category_id, t.name, t.description, t.image_url, TRUE
FROM tag_categories tc
CROSS JOIN (VALUES
    ('Máy lạnh', 'Hệ thống điều hòa nhiệt độ mát mẻ', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749477/moodcafe/tags/amenity_aircon.jpg'),
    ('Wifi', 'Mạng không dây kết nối Internet', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749478/moodcafe/tags/amenity_wifi.jpg'),
    ('Ổ cắm điện', 'Điểm sạc pin cho thiết bị điện tử', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749480/moodcafe/tags/amenity_power_socket.jpg'),
    ('Bãi đỗ xe máy', 'Khu vực để xe gắn máy thuận tiện', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749481/moodcafe/tags/amenity_motorbike_parking.jpg'),
    ('Bãi đỗ ô tô', 'Chỗ đậu xe hơi an toàn', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749483/moodcafe/tags/amenity_car_parking.jpg'),
    ('Thân thiện thú cưng', 'Cho phép mang theo chó mèo vào quán', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749484/moodcafe/tags/amenity_pet_friendly.jpg'),
    ('Nước lọc miễn phí', 'Cung cấp nước uống miễn phí tại chỗ', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749549/moodcafe/tags/amenity_free_water.jpg'),
    ('Bàn lớn làm việc', 'Bàn dài rộng rãi phục vụ nhóm đông', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749486/moodcafe/tags/amenity_large_desk.jpg'),
    ('Khu vực ngoài trời', 'Không gian mở đón gió và khí trời', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749487/moodcafe/tags/amenity_outdoor_area.jpg'),
    ('Ghế êm ái', 'Chỗ ngồi sofa đệm êm dễ chịu', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749489/moodcafe/tags/amenity_comfortable_seats.jpg'),
    ('Không hút thuốc', 'Môi trường trong lành cấm khói thuốc', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749490/moodcafe/tags/amenity_no_smoking.jpg'),
    ('Thanh toán thẻ/QR', 'Chấp nhận quét mã QR và quẹt thẻ', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749550/moodcafe/tags/amenity_qr_card_payment.jpg'),
    ('Phục vụ bánh ngọt', 'Có bán kèm bánh mì, bánh ngọt tươi ngon', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749492/moodcafe/tags/amenity_pastry_served.jpg'),
    ('Mở cửa khuya', 'Hoạt động đến nửa đêm hoặc sáng hôm sau', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749494/moodcafe/tags/amenity_late_night.jpg'),
    ('Nhà vệ sinh riêng', 'Khu vực vệ sinh sạch sẽ, riêng tư', 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789749495/moodcafe/tags/amenity_private_restroom.jpg')
) AS t(name, description, image_url)
WHERE tc.code = 'AMENITY'
ON CONFLICT (name) WHERE is_deleted = FALSE DO UPDATE 
SET image_url = EXCLUDED.image_url, description = EXCLUDED.description, is_active = TRUE;
