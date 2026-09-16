-- =========================================================================
-- V15: STANDARDIZE 11 ARCHITECTURAL & INTERIOR VIBE TAGS WITH CLOUDINARY ASSETS
-- =========================================================================

-- 1. Ensure image_url column exists in tags table
ALTER TABLE tags ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

-- 2. Insert or update the 11 standardized architectural & interior vibe tags with real Cloudinary image URLs
INSERT INTO tags (name, description, tag_category_id, image_url, is_active)
VALUES
    (
        'Tối giản (Minimalism / Wabi-sabi)',
        'Không gian tinh gọn, đơn sắc, khoảng thở lớn, nội thất thanh mảnh, ít chi tiết thừa',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584329/moodcafe/vibes/minimalist.jpg',
        TRUE
    ),
    (
        'Cổ điển (Vintage / Retro)',
        'Hoài niệm phong cách xưa cũ, đồ gỗ trầm ấm, ánh đèn vàng ấm cúng',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584339/moodcafe/vibes/vintage.jpg',
        TRUE
    ),
    (
        'Phong cách Nhật Bản (Japanese / Zen)',
        'Gỗ sáng màu, vách ngăn thanh tao, sỏi đá, thanh tịnh và tinh tế',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584326/moodcafe/vibes/japanese.jpg',
        TRUE
    ),
    (
        'Sân vườn nhiệt đới (Tropical Garden)',
        'Nhiều cây xanh, ánh sáng tự nhiên, hồ cá, gió trời thoáng đãng',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584337/moodcafe/vibes/tropical.jpg',
        TRUE
    ),
    (
        'Công xưởng (Industrial)',
        'Tường gạch thô, kim loại trần, trần cao, sàn bê tông mài cá tính',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584325/moodcafe/vibes/industrial.jpg',
        TRUE
    ),
    (
        'Gỗ mộc ấm cúng (Rustic Wood)',
        'Nội thất gỗ tự nhiên mộc mạc, ánh đèn vàng dịu, cảm giác che chở ấm áp',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584335/moodcafe/vibes/rustic.jpg',
        TRUE
    ),
    (
        'Hiện đại & Sang trọng (Modern Luxury)',
        'Mặt kính lớn, đá marble, kim loại ánh kim, bàn ghế cao cấp',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584331/moodcafe/vibes/modern.jpg',
        TRUE
    ),
    (
        'Đông Dương (Indochine)',
        'Nền gạch bông, quạt trần, gỗ đen, giao thoa nét đẹp Á - Âu hoài cổ',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584323/moodcafe/vibes/indochine.jpg',
        TRUE
    ),
    (
        'Hàn Quốc (Korean Aesthetic)',
        'Tông màu pastel trong trẻo, ánh sáng mềm, nhiều góc check-in tinh tế',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584328/moodcafe/vibes/korean.jpg',
        TRUE
    ),
    (
        'Nghệ thuật (Art Studio)',
        'Không gian trưng bày tranh vẽ, tượng điêu khắc, ánh sáng khơi nguồn cảm hứng',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584320/moodcafe/vibes/art-studio.jpg',
        TRUE
    ),
    (
        'Tầng thượng (Rooftop / Open Air)',
        'Không gian mở đón gió, ngắm hoàng hôn lung linh và phố xá từ trên cao',
        (SELECT tag_category_id FROM tag_categories WHERE code = 'VIBE'),
        'https://res.cloudinary.com/dy45rrkhf/image/upload/v1789584333/moodcafe/vibes/rooftop.jpg',
        TRUE
    )
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    tag_category_id = EXCLUDED.tag_category_id,
    image_url = EXCLUDED.image_url,
    is_active = TRUE;

-- 3. Migrate store_tags from legacy vibes to refined architectural vibes
DO $$
DECLARE
    v_vibe_cat UUID;
BEGIN
    SELECT tag_category_id INTO v_vibe_cat FROM tag_categories WHERE code = 'VIBE';

    -- Map 'Ấm cúng & Gỗ mộc' -> 'Gỗ mộc ấm cúng (Rustic Wood)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Gỗ mộc ấm cúng (Rustic Wood)' LIMIT 1)
    WHERE tag_id = (SELECT tag_id FROM tags WHERE name = 'Ấm cúng & Gỗ mộc' LIMIT 1);

    -- Map 'Cây xanh & Thoáng đãng' -> 'Sân vườn nhiệt đới (Tropical Garden)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Sân vườn nhiệt đới (Tropical Garden)' LIMIT 1)
    WHERE tag_id = (SELECT tag_id FROM tags WHERE name = 'Cây xanh & Thoáng đãng' LIMIT 1);

    -- Map 'Cổ điển (Vintage)' & 'Vintage' -> 'Cổ điển (Vintage / Retro)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Cổ điển (Vintage / Retro)' LIMIT 1)
    WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Cổ điển (Vintage)', 'Vintage'));

    -- Map 'Tối giản (Minimalist)' & 'Minimalist' -> 'Tối giản (Minimalism / Wabi-sabi)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Tối giản (Minimalism / Wabi-sabi)' LIMIT 1)
    WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Tối giản (Minimalist)', 'Minimalist'));

    -- Map 'Industrial' -> 'Công xưởng (Industrial)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Công xưởng (Industrial)' LIMIT 1)
    WHERE tag_id IN (SELECT tag_id FROM tags WHERE name = 'Industrial');

    -- Map 'Sân vườn / Botanical' -> 'Sân vườn nhiệt đới (Tropical Garden)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Sân vườn nhiệt đới (Tropical Garden)' LIMIT 1)
    WHERE tag_id IN (SELECT tag_id FROM tags WHERE name = 'Sân vườn / Botanical');

    -- Map 'Hiện đại / Modern' & 'Hiện đại & Sang trọng' -> 'Hiện đại & Sang trọng (Modern Luxury)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Hiện đại & Sang trọng (Modern Luxury)' LIMIT 1)
    WHERE tag_id IN (SELECT tag_id FROM tags WHERE name IN ('Hiện đại & Sang trọng', 'Hiện đại / Modern'));

    -- Map 'Nghệ thuật & Sáng tạo' -> 'Nghệ thuật (Art Studio)'
    UPDATE store_tags
    SET tag_id = (SELECT tag_id FROM tags WHERE name = 'Nghệ thuật (Art Studio)' LIMIT 1)
    WHERE tag_id = (SELECT tag_id FROM tags WHERE name = 'Nghệ thuật & Sáng tạo' LIMIT 1);

    -- Deactivate ALL legacy, subjective, or unstandardized vibe tags
    UPDATE tags
    SET is_active = FALSE
    WHERE tag_category_id = v_vibe_cat
      AND name IN (
          'Vintage',
          'Minimalist',
          'Industrial',
          'Sân vườn / Botanical',
          'Hiện đại / Modern',
          'Ấm cúng & Gỗ mộc',
          'Cây xanh & Thoáng đãng',
          'Cổ điển (Vintage)',
          'Tối giản (Minimalist)',
          'Nghệ thuật & Sáng tạo',
          'Hiện đại & Sang trọng',
          'Chill Lofi & Nhẹ nhàng',
          'Chữa lành (Healing)',
          'Năng động & Trẻ trung',
          'Lãng mạn & Thơ mộng'
      );
END $$;
