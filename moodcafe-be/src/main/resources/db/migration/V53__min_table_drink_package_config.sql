-- =========================================================================
-- V53: ADD CONFIGURATION FOR MINIMUM TABLE DRINK PACKAGE AMOUNT
-- Configurable minimum prepaid drink package amount required for table categories.
-- =========================================================================

INSERT INTO system_configurations
    (config_group, config_key, config_value, data_type, display_name, description, is_public)
SELECT
    'BOOKING', 'MIN_TABLE_DRINK_PACKAGE_VND', '20000', 'NUMBER',
    'Giá trị gói đồ uống trả trước tối thiểu (VNĐ)',
    'Mức giá tối thiểu mà chủ quán phải thiết lập cho gói đồ uống trả trước khi tạo/cập nhật loại bàn đặt trước qua App.',
    true
WHERE NOT EXISTS (
    SELECT 1 FROM system_configurations WHERE config_key = 'MIN_TABLE_DRINK_PACKAGE_VND'
);

UPDATE system_configurations
SET config_value = '20000',
    display_name = 'Giá trị gói đồ uống trả trước tối thiểu (VNĐ)',
    description = 'Mức giá tối thiểu mà chủ quán phải thiết lập cho gói đồ uống trả trước khi tạo/cập nhật loại bàn đặt trước qua App.',
    is_public = true,
    is_deleted = false
WHERE config_key = 'MIN_TABLE_DRINK_PACKAGE_VND';
