INSERT INTO system_configurations
    (config_group, config_key, config_value, data_type, display_name, description, is_public)
VALUES
    ('FINANCE', 'MIN_WITHDRAWAL_AMOUNT_VND', '100000', 'NUMBER',
     'Số tiền rút tối thiểu (VNĐ)',
     'Hạn mức tối thiểu để chủ quán tạo lệnh rút tiền. Chỉ áp dụng cho lệnh tạo sau khi thay đổi.',
     true)
ON CONFLICT DO NOTHING;
