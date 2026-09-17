-- ====================================================================
-- Flyway Migration: V16__update_tag_descriptions_for_trending_display.sql
-- Description: Cập nhật nội dung mô tả (description) cho các thẻ Tag (đặc biệt là NOISE, AMENITY, VIBE, PURPOSE)
--              để khi hiển thị trên giao diện City Trending ("Cho tôi xem thành phố đang có gì")
--              thì phụ đề (description) luôn trau chuốt, tự nhiên, hấp dẫn và không bị cụt lủn hay lặp lại tên thẻ.
-- ====================================================================

-- 1. Cập nhật nhóm Độ ồn (NOISE)
UPDATE tags SET description = 'Không gian thanh bình, âm lượng nói chuyện nhẹ nhàng, lý tưởng để học tập và thư giãn'
WHERE name = 'Yên tĩnh';

UPDATE tags SET description = 'Không gian êm dịu, tiếng trò chuyện khẽ khàng hòa cùng giai điệu nhạc nền thư thái'
WHERE name = 'Khá yên tĩnh';

UPDATE tags SET description = 'Âm lượng vừa phải tự nhiên, nhạc nền nhẹ nhàng, thoải mái cho mọi nhu cầu gặp gỡ'
WHERE name = 'Bình thường';

UPDATE tags SET description = 'Không khí tươi vui rộn rã, âm nhạc trẻ trung, phù hợp hẹn hò nhóm và trò chuyện thoải mái'
WHERE name = 'Khá sôi động';

UPDATE tags SET description = 'Không gian ngập tràn năng lượng, âm nhạc bắt tai, lý tưởng để tụ tập và chuyện trò hứng khởi'
WHERE name = 'Sôi động';

-- Xóa 3 tag độ ồn cũ trùng lặp không sử dụng (Yên tĩnh tuyệt đối, Vừa phải, Náo nhiệt)
DELETE FROM store_tags WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Yên tĩnh tuyệt đối', 'Vừa phải', 'Náo nhiệt'));
DELETE FROM user_preferences WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Yên tĩnh tuyệt đối', 'Vừa phải', 'Náo nhiệt'));
DELETE FROM tag_ratings WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Yên tĩnh tuyệt đối', 'Vừa phải', 'Náo nhiệt'));
DELETE FROM tags WHERE name IN ('Yên tĩnh tuyệt đối', 'Vừa phải', 'Náo nhiệt');


-- 2. Cập nhật nhóm Tiện ích (AMENITY)
UPDATE tags SET description = 'Không gian máy lạnh mát mẻ, thoáng đãng, mang lại cảm giác dễ chịu suốt ngày dài'
WHERE name = 'Máy lạnh';

UPDATE tags SET description = 'Hệ thống điều hòa mát sâu, nhiệt độ lý tưởng giúp bạn thoải mái nghỉ ngơi và làm việc'
WHERE name = 'Máy lạnh mát sâu';

UPDATE tags SET description = 'Nhiều ổ cắm điện bố trí thuận tiện tại nhiều vị trí, dễ dàng sạc laptop và điện thoại'
WHERE name = 'Ổ điện';

UPDATE tags SET description = 'Bố trí nhiều ổ cắm tiện lợi tại từng chỗ ngồi, thoải mái sạc pin làm việc cả ngày'
WHERE name = 'Ổ cắm điện từng bàn';

UPDATE tags SET description = 'Đường truyền Wi-Fi ổn định, lướt web và làm việc trực tuyến nhanh chóng mượt mà'
WHERE name = 'Wi-Fi';

UPDATE tags SET description = 'Đường truyền cáp quang mạnh mẽ, ổn định cho làm việc online và họp video mượt mà'
WHERE name = 'Wifi tốc độ cao';

UPDATE tags SET description = 'Bãi giữ xe máy rộng thoáng, an toàn, có bảo vệ hỗ trợ nhiệt tình'
WHERE name = 'Chỗ để xe máy';

UPDATE tags SET description = 'Có chỗ đỗ ô tô an toàn, rộng rãi ngay trước quán hoặc bãi gần kề'
WHERE name = 'Chỗ đỗ ô tô';

UPDATE tags SET description = 'Bãi đỗ xe rộng rãi, thuận tiện cho cả xe máy và ô tô với an ninh đảm bảo'
WHERE name = 'Bãi đỗ xe';

UPDATE tags SET description = 'Cung cấp nhiều bộ trò chơi boardgame hấp dẫn, tăng thêm niềm vui khi tụ tập bạn bè'
WHERE name = 'Boardgame';

UPDATE tags SET description = 'Quán chào đón thú cưng, thoải mái dắt theo bạn bốn chân cùng đi cà phê'
WHERE name = 'Thân thiện thú cưng';

UPDATE tags SET description = 'Thân thiện với thú cưng, không gian thoải mái cho bạn và cún mèo cưng nựng'
WHERE name = 'Thú cưng';

UPDATE tags SET description = 'Góc ngồi ngoài trời mát mẻ đón gió tự nhiên, ngắm nhìn phố xá thư thái'
WHERE name = 'Khu vực ngoài trời';

UPDATE tags SET description = 'Khuôn viên sân vườn xanh mát ngập tràn hoa lá, không khí trong lành tự nhiên'
WHERE name = 'Có sân vườn';

UPDATE tags SET description = 'Nhiều góc ngồi có view ngắm cảnh tuyệt đẹp, bàn ghế đệm êm ái thư thái'
WHERE name = 'Chỗ ngồi đẹp';

UPDATE tags SET description = 'Thực đơn đa dạng với bánh ngọt nướng thơm ngon và các món ăn nhẹ hấp dẫn'
WHERE name = 'Đồ ăn';

UPDATE tags SET description = 'Luôn phục vụ sẵn bình nước lọc mát lành hoặc nước detox trái cây miễn phí'
WHERE name = 'Nước lọc miễn phí';

UPDATE tags SET description = 'Thời gian mở cửa qua đêm hoặc sau 23h, lý tưởng cho cú đêm làm việc và trò chuyện'
WHERE name = 'Mở cửa khuya';

UPDATE tags SET description = 'Hỗ trợ thanh toán linh hoạt qua quét mã VietQR, ví điện tử và các loại thẻ ngân hàng'
WHERE name IN ('Thanh toán QR / Thẻ', 'Thanh toán thẻ / QR');

UPDATE tags SET description = 'Khu vực máy lạnh và phòng kín hoàn toàn không khói thuốc, không khí trong lành dễ chịu'
WHERE name = 'Không hút thuốc';

UPDATE tags SET description = 'Không gian phòng kín cách âm riêng tư, có màn hình hỗ trợ họp nhóm hiệu quả'
WHERE name = 'Phòng riêng / Họp';

UPDATE tags SET description = 'Bàn dài và khu vực rộng rãi, chuyên biệt cho tổ chức hội thảo và workshop sáng tạo'
WHERE name = 'Workshop';


-- 3. Cập nhật nhóm Phong cách (VIBE) & Mục đích (PURPOSE) còn ngắn
UPDATE tags SET description = 'Đường nét tinh giản, nội thất sang trọng mang hơi thở đô thị thời thượng'
WHERE name = 'Hiện đại / Modern';

UPDATE tags SET description = 'Phong cách hoài cổ với gam màu ấm áp, đưa bạn tìm về những ký ức xưa êm đềm'
WHERE name = 'Vintage';

UPDATE tags SET description = 'Không gian công xưởng cá tính với trần cao thoáng đãng, gạch thô và kim loại mộc mạc'
WHERE name = 'Industrial';

UPDATE tags SET description = 'Thiết kế tối giản tinh tế, khoảng thở thoáng rộng mang đến sự tĩnh lặng và thư thái'
WHERE name = 'Minimalist';

UPDATE tags SET description = 'Khu vườn xanh mướt ngập tràn cây cối nhiệt đới, không khí trong lành và gần gũi thiên nhiên'
WHERE name = 'Sân vườn / Botanical';

UPDATE tags SET description = 'Không gian dịu dàng với ánh đèn lung linh, hoa tươi và những giai điệu tình ca ngọt ngào'
WHERE name = 'Lãng mạn & Thơ mộng';

UPDATE tags SET description = 'Giai điệu du dương, ghế ngồi êm ái, mang lại những phút giây thư giãn thảnh thơi'
WHERE name = 'Thư giãn / Đọc sách';

UPDATE tags SET description = 'Không gian yên bình thoang thoảng hương cà phê, lý tưởng để đắm chìm vào từng trang sách'
WHERE name = 'Đọc sách & Thư giãn';

UPDATE tags SET description = 'Không gian an yên, tách biệt khỏi phố thị ồn ã, vỗ về cảm xúc và tái tạo năng lượng'
WHERE name = 'Chữa lành & Trầm lắng';
