-- =========================================================
-- V13: SEED STANDARD MASTER TAGS FOR QUICK MOOD MATCHER (15S)
-- Seed 10 Purpose Tags, 15 Amenity Tags, 5 Noise Tags, and 11 Vibe Tags
-- =========================================================

-- 1. Seed 10 Standard Purpose Tags
INSERT INTO tags (name, description, tag_category_id, is_active)
VALUES
    ('Học tập & Làm việc', 'Cần chỗ tập trung cao độ, yên tĩnh, bàn cao và ổ điện', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Tụ tập bạn bè', 'Trò chuyện thoải mái, không khí vui vẻ, sôi nổi', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Hẹn hò lãng mạn', 'Không gian ấm cúng, riêng tư, ánh đèn dịu nhẹ', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Đọc sách & Thư giãn', 'Nhạc êm dịu, ghế tựa thoải mái, thảnh thơi', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Làm việc nhóm / Họp', 'Bàn lớn, không gian thảo luận trao đổi thuận tiện', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Chụp ảnh & Check-in', 'Nhiều góc check-in đẹp, ánh sáng tự nhiên nghệ thuật', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Chữa lành & Trầm lắng', 'Không gian yên ả, nhẹ nhàng giúp xoa dịu tâm trí', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Gặp gỡ đối tác', 'Lịch sự, chuyên nghiệp, không gian sang trọng', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Thư giãn ngoài trời', 'Sân vườn thoáng mát, nhiều cây xanh gần gũi thiên nhiên', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE),
    ('Chơi Boardgame & Giải trí', 'Không gian vui nhộn, bàn rộng rãi, đồ uống đa dạng', (SELECT tag_category_id FROM tag_categories WHERE code = 'PURPOSE'), TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    tag_category_id = EXCLUDED.tag_category_id,
    is_active = TRUE;

-- 2. Seed 15 Standard Amenity Tags
INSERT INTO tags (name, description, tag_category_id, is_active)
VALUES
    ('Ổ điện', 'Ổ cắm điện thuận tiện bố trí tại nhiều vị trí', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Wi-Fi', 'Mạng Wi-Fi internet không dây miễn phí', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Chỗ ngồi đẹp', 'Ghế đệm êm ái, góc ngồi view thoáng dễ chịu', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Có sân vườn', 'Không gian ngoài trời nhiều mảng xanh tươi mát', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Máy lạnh', 'Hệ thống điều hòa làm mát ổn định toàn bộ không gian', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Bàn đơn', 'Bàn riêng dành cho 1 người ngồi làm việc hoặc đọc sách', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Boardgame', 'Trang bị sẵn trò chơi bàn giải trí nhóm', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Workshop', 'Không gian hoặc bàn dài phục vụ tổ chức hội thảo, workshop', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Đồ ăn', 'Phục vụ các món ăn nhẹ, bánh ngọt hoặc điểm tâm', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Mở cửa khuya', 'Thời gian hoạt động qua đêm hoặc sau 23h', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Bãi đỗ xe', 'Khu vực đỗ xe máy hoặc ô tô an ninh, thuận tiện', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Thú cưng', 'Cho phép mang thú cưng hoặc quán có nuôi thú cưng thân thiện', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Thanh toán thẻ / QR', 'Hỗ trợ chuyển khoản QR code, thẻ tín dụng, ví điện tử', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Phòng riêng / Họp', 'Phòng cách âm có cửa riêng cho các cuộc họp hoặc thảo luận', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE),
    ('Không hút thuốc', 'Khu vực máy lạnh và phòng kín hoàn toàn không khói thuốc', (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY'), TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    tag_category_id = EXCLUDED.tag_category_id,
    is_active = TRUE;

-- 3. Seed 5 Standard Noise Scale Tags (Scale 1..5)
INSERT INTO tags (name, description, tag_category_id, scale_value, is_active)
VALUES
    ('Yên tĩnh', 'Yên tĩnh', (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'), 1, TRUE),
    ('Khá yên tĩnh', 'Khá yên tĩnh', (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'), 2, TRUE),
    ('Bình thường', 'Bình thường', (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'), 3, TRUE),
    ('Khá sôi động', 'Khá sôi động', (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'), 4, TRUE),
    ('Sôi động', 'Sôi động', (SELECT tag_category_id FROM tag_categories WHERE code = 'NOISE'), 5, TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    tag_category_id = EXCLUDED.tag_category_id,
    scale_value = EXCLUDED.scale_value,
    is_active = TRUE;

-- 4. Seed 11 Standard Vibe Tags
INSERT INTO tags (name, description, tag_category_id, is_active)
VALUES
    ('Ấm cúng & Gỗ mộc', 'Nội thất gỗ trầm ấm, ánh đèn vàng thư thái', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Cây xanh & Thoáng đãng', 'Nhiều cây xanh nhiệt đới, không khí trong lành gần gũi thiên nhiên', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Cổ điển (Vintage)', 'Hoài niệm phong cách retro hoài cổ, đồ trang trí xưa cũ', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Tối giản (Minimalist)', 'Không gian tinh giản, gọn gàng, ít chi tiết thừa', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Nghệ thuật & Sáng tạo', 'Trưng bày tranh ảnh, decor độc đáo, khơi nguồn cảm hứng', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Chill Lofi & Nhẹ nhàng', 'Giai điệu lofi dịu êm, thư giãn êm đềm', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Chữa lành (Healing)', 'Không gian thiền tịnh, xoa dịu tâm trí, bình an', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Hiện đại & Sang trọng', 'Thiết kế thanh lịch, cao cấp, thời thượng', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Công xưởng (Industrial)', 'Tường gạch thô, kim loại trần cá tính mạnh mẽ', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Năng động & Trẻ trung', 'Âm nhạc sôi nổi, sắc màu tươi sáng tràn đầy năng lượng', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE),
    ('Lãng mạn & Thơ mộng', 'Góc ngồi êm đềm, nến và hoa dịu dàng', (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'), TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    tag_category_id = EXCLUDED.tag_category_id,
    is_active = TRUE;
