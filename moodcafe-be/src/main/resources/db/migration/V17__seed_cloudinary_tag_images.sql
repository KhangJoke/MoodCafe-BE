-- ====================================================================
-- Flyway Migration: V17__seed_cloudinary_tag_images.sql
-- Description: Seed toàn bộ image_url từ Cloudinary (cloud: dy45rrkhf)
--              cho tất cả các thẻ Tag trong hệ thống (PURPOSE, NOISE, AMENITY, VIBE)
--              đảm bảo không còn thẻ nào có image_url bị NULL hoặc rỗng.
-- ====================================================================

-- 1. Nhóm ĐỘ ỒN (NOISE)
UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630509/moodcafe/tags/noise_quiet.jpg'
WHERE name IN ('Yên tĩnh', 'Khá yên tĩnh');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630469/moodcafe/tags/noise_normal.jpg'
WHERE name = 'Bình thường';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630469/moodcafe/tags/noise_lively.jpg'
WHERE name IN ('Khá sôi động', 'Sôi động');


-- 2. Nhóm MỤC ĐÍCH (PURPOSE)
UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630459/moodcafe/tags/study_deadline.jpg'
WHERE name IN ('Học bài / Chạy deadline', 'Học tập & Làm việc');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630460/moodcafe/tags/group_work.jpg'
WHERE name IN ('Làm việc nhóm', 'Làm việc nhóm / Họp');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630461/moodcafe/tags/dating_romantic.jpg'
WHERE name = 'Hẹn hò lãng mạn';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630462/moodcafe/tags/reading_book.jpg'
WHERE name IN ('Thư giãn / Đọc sách', 'Đọc sách & Thư giãn');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630463/moodcafe/tags/friends_hangout.jpg'
WHERE name = 'Tụ tập bạn bè';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630464/moodcafe/tags/boardgame.jpg'
WHERE name IN ('Tụ tập bạn bè / Boardgame', 'Chơi Boardgame & Giải trí');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630465/moodcafe/tags/photo_checkin.jpg'
WHERE name IN ('Chụp ảnh & Check-in', 'Chụp ảnh / Sống ảo');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630465/moodcafe/tags/healing_calm.jpg'
WHERE name = 'Chữa lành & Trầm lắng';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630466/moodcafe/tags/business_meeting.jpg'
WHERE name = 'Gặp gỡ đối tác';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630508/moodcafe/tags/outdoor_garden.jpg'
WHERE name = 'Thư giãn ngoài trời';


-- 3. Nhóm TIỆN ÍCH (AMENITY)
UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630472/moodcafe/tags/air_conditioner.jpg'
WHERE name IN ('Máy lạnh', 'Máy lạnh mát sâu');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630471/moodcafe/tags/power_outlet.jpg'
WHERE name IN ('Ổ điện', 'Ổ cắm điện từng bàn');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630470/moodcafe/tags/wifi_connection.jpg'
WHERE name IN ('Wi-Fi', 'Wifi tốc độ cao');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630479/moodcafe/tags/parking_space.jpg'
WHERE name IN ('Bãi đỗ xe', 'Chỗ để xe máy', 'Chỗ đỗ ô tô');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630473/moodcafe/tags/pet_friendly.jpg'
WHERE name IN ('Thú cưng', 'Thân thiện thú cưng');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630474/moodcafe/tags/night_owl.jpg'
WHERE name = 'Mở cửa khuya';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630475/moodcafe/tags/good_view_seats.jpg'
WHERE name = 'Chỗ ngồi đẹp';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630476/moodcafe/tags/single_desk.jpg'
WHERE name = 'Bàn đơn';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630477/moodcafe/tags/private_room.jpg'
WHERE name = 'Phòng riêng / Họp';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630478/moodcafe/tags/non_smoking.jpg'
WHERE name = 'Không hút thuốc';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630510/moodcafe/tags/digital_payment.jpg'
WHERE name IN ('Thanh toán QR / Thẻ', 'Thanh toán thẻ / QR');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630480/moodcafe/tags/pastries_food.jpg'
WHERE name = 'Đồ ăn';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630511/moodcafe/tags/free_water.jpg'
WHERE name = 'Nước lọc miễn phí';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630508/moodcafe/tags/outdoor_garden.jpg'
WHERE name IN ('Có sân vườn', 'Khu vực ngoài trời');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630481/moodcafe/tags/workshop.jpg'
WHERE name = 'Workshop';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630464/moodcafe/tags/boardgame.jpg'
WHERE name = 'Boardgame';


-- 4. Nhóm PHONG CÁCH (VIBE) còn thiếu
UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584339/moodcafe/vibes/vintage.jpg'
WHERE name IN ('Cổ điển (Vintage)', 'Vintage');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584325/moodcafe/vibes/industrial.jpg'
WHERE name = 'Industrial';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584329/moodcafe/vibes/minimalist.jpg'
WHERE name IN ('Minimalist', 'Tối giản (Minimalist)');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584331/moodcafe/vibes/modern.jpg'
WHERE name IN ('Hiện đại / Modern', 'Hiện đại & Sang trọng');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584335/moodcafe/vibes/rustic.jpg'
WHERE name = 'Ấm cúng & Gỗ mộc';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584337/moodcafe/vibes/tropical.jpg'
WHERE name IN ('Sân vườn / Botanical', 'Cây xanh & Thoáng đãng');

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584320/moodcafe/vibes/art-studio.jpg'
WHERE name = 'Nghệ thuật & Sáng tạo';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630482/moodcafe/tags/chill_lofi.jpg'
WHERE name = 'Chill Lofi & Nhẹ nhàng';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630465/moodcafe/tags/healing_calm.jpg'
WHERE name = 'Chữa lành (Healing)';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630461/moodcafe/tags/dating_romantic.jpg'
WHERE name = 'Lãng mạn & Thơ mộng';

UPDATE tags SET image_url = 'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789630483/moodcafe/tags/energetic_vibrant.jpg'
WHERE name = 'Năng động & Trẻ trung';
