-- =========================================================
-- V14: CLEANUP LEGACY VERBOSE AMENITY TAGS
-- Deactivate redundant legacy verbose tags from V12 so that
-- the 15 standardized concise amenity tags are returned cleanly
-- =========================================================

-- Deactivate legacy verbose duplicate tags
UPDATE tags
SET is_active = FALSE
WHERE name IN (
    'Wifi tốc độ cao',
    'Ổ cắm điện từng bàn',
    'Máy lạnh mát sâu',
    'Chỗ để xe máy',
    'Chỗ đỗ ô tô',
    'Thân thiện thú cưng',
    'Khu vực ngoài trời',
    'Nước lọc miễn phí',
    'Thanh toán QR / Thẻ'
) AND tag_category_id = (SELECT tag_category_id FROM tag_categories WHERE code = 'AMENITY');
