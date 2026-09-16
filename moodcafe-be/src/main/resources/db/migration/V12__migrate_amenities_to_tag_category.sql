-- =========================================================
-- V12: REFACTOR AMENITIES TO STANDARDIZED TAG CATEGORY (AMENITY)
-- Migrate store amenities from store module into tag module,
-- and drop legacy amenities & store_amenities tables.
-- =========================================================

-- 1. Insert Tag Category for Amenities
INSERT INTO tag_categories (name, code, approval_mode, control_type, display_order, is_active)
VALUES ('Tiện ích quán', 'AMENITY', 'OWNER_REQUEST', 'TAG_LIST', 4, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    approval_mode = EXCLUDED.approval_mode,
    control_type = EXCLUDED.control_type,
    display_order = EXCLUDED.display_order,
    is_active = EXCLUDED.is_active;

-- 2. Migrate existing data from amenities to tags (if amenities table exists)
DO $$
DECLARE
    v_amenity_cat_id UUID;
BEGIN
    SELECT tag_category_id INTO v_amenity_cat_id
    FROM tag_categories
    WHERE code = 'AMENITY';

    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'amenities'
    ) THEN
        -- Copy all existing amenities into tags under category AMENITY
        INSERT INTO tags (name, description, tag_category_id, is_active)
        SELECT a.name, a.description, v_amenity_cat_id, TRUE
        FROM amenities a
        ON CONFLICT (name) DO UPDATE
        SET tag_category_id = v_amenity_cat_id
        WHERE tags.tag_category_id IS NULL;
    END IF;

    -- 3. Seed standard amenities if not already present
    INSERT INTO tags (name, description, tag_category_id, is_active)
    VALUES
        ('Wifi tốc độ cao', 'Đường truyền cáp quang riêng biệt, kết nối ổn định', v_amenity_cat_id, TRUE),
        ('Ổ cắm điện từng bàn', 'Nhiều ổ cắm thuận tiện cho làm việc và sạc thiết bị', v_amenity_cat_id, TRUE),
        ('Máy lạnh mát sâu', 'Không gian điều hòa nhiệt độ thoải mái', v_amenity_cat_id, TRUE),
        ('Chỗ để xe máy', 'Bãi đỗ xe máy thuận tiện, có người trông coi', v_amenity_cat_id, TRUE),
        ('Chỗ đỗ ô tô', 'Có bãi đỗ hoặc hỗ trợ gửi ô tô gần quán', v_amenity_cat_id, TRUE),
        ('Thân thiện thú cưng', 'Chào đón thú cưng tại khu vực cho phép', v_amenity_cat_id, TRUE),
        ('Khu vực ngoài trời', 'Không gian sân vườn hoặc ban công thoáng đãng', v_amenity_cat_id, TRUE),
        ('Phòng riêng / Họp', 'Phòng họp hoặc khu vực riêng tư cho nhóm', v_amenity_cat_id, TRUE),
        ('Nước lọc miễn phí', 'Luôn sẵn bình nước detox hoặc nước lọc tự phục vụ', v_amenity_cat_id, TRUE),
        ('Thanh toán QR / Thẻ', 'Chấp nhận chuyển khoản VietQR, Momo và thẻ tín dụng', v_amenity_cat_id, TRUE),
        ('Không hút thuốc', 'Không gian trong nhà nghiêm cấm hút thuốc', v_amenity_cat_id, TRUE)
    ON CONFLICT (name) DO NOTHING;

    -- 4. Migrate store_amenities associations into store_tags as APPROVED
    IF EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'store_amenities'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'amenities'
    ) THEN
        INSERT INTO store_tags (store_id, tag_id, status, approved_at, created_at)
        SELECT
            sa.store_id,
            t.tag_id,
            'APPROVED',
            COALESCE(sa.created_at, CURRENT_TIMESTAMP),
            COALESCE(sa.created_at, CURRENT_TIMESTAMP)
        FROM store_amenities sa
        JOIN amenities a ON sa.amenity_id = a.amenity_id
        JOIN tags t ON t.name = a.name
        ON CONFLICT (store_id, tag_id) DO NOTHING;
    END IF;
END $$;

-- 5. Drop legacy store_amenities and amenities tables
DROP TABLE IF EXISTS store_amenities CASCADE;
DROP TABLE IF EXISTS amenities CASCADE;
