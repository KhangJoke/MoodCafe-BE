-- V18: Create system_configurations table and seed default configs

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS system_configurations (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_group VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

CREATE INDEX IF NOT EXISTS idx_system_configs_group ON system_configurations(config_group);
CREATE INDEX IF NOT EXISTS idx_system_configs_key ON system_configurations(config_key);
CREATE INDEX IF NOT EXISTS idx_system_configs_public ON system_configurations(is_public);

-- Seed default configurations for MoodCafe
INSERT INTO system_configurations (config_group, config_key, config_value, data_type, display_name, description, is_public)
VALUES
    -- 1. MATCH_SCORE (Trọng số thuật toán gợi ý theo Gu cá nhân)
    ('MATCH_SCORE', 'MATCH_WEIGHT_VIBE', '0.30', 'NUMBER', 'Trọng số Phong cách (Vibe)', 'Tỷ lệ % độ phù hợp phong cách không gian giữa quán và gu khách (0.0 - 1.0)', true),
    ('MATCH_SCORE', 'MATCH_WEIGHT_PURPOSE', '0.30', 'NUMBER', 'Trọng số Mục đích ghé quán', 'Tỷ lệ % độ phù hợp mục đích sử dụng chính như học tập, hẹn hò, đọc sách (0.0 - 1.0)', true),
    ('MATCH_SCORE', 'MATCH_WEIGHT_NOISE', '0.15', 'NUMBER', 'Trọng số Mức độ ồn', 'Tỷ lệ % độ tương thích giữa độ nhạy cảm âm thanh của khách và độ ồn thực tế quán (0.0 - 1.0)', true),
    ('MATCH_SCORE', 'MATCH_WEIGHT_AMENITY', '0.15', 'NUMBER', 'Trọng số Tiện ích thiết yếu', 'Tỷ lệ % đáp ứng các tiện ích bắt buộc mà khách mong muốn (0.0 - 1.0)', true),
    ('MATCH_SCORE', 'MATCH_WEIGHT_RATING', '0.10', 'NUMBER', 'Trọng số Điểm đánh giá thực tế', 'Tỷ lệ % điểm uy tín trung bình từ cộng đồng đánh giá (0.0 - 1.0)', true),

    -- 2. STORE_SLA (Quy chuẩn cam kết vận hành và thẩm định)
    ('STORE_SLA', 'SLA_TAG_REQUEST_HOURS', '48', 'NUMBER', 'Thời hạn SLA duyệt thẻ Vibe (giờ)', 'Số giờ tối đa Admin phải thẩm định yêu cầu xin cấp thẻ của chủ quán', false),
    ('STORE_SLA', 'SLA_STORE_ONBOARDING_HOURS', '72', 'NUMBER', 'Thời hạn SLA duyệt hồ sơ quán mới (giờ)', 'Số giờ tối đa Admin phải duyệt hoặc phản hồi hồ sơ đăng ký quán mới', false),

    -- 3. DISCOVERY (Cấu hình hiển thị trang tìm kiếm và khám phá)
    ('DISCOVERY', 'DISCOVERY_DEFAULT_PAGE_SIZE', '12', 'NUMBER', 'Số quán hiển thị mặc định mỗi trang', 'Số lượng thẻ quán hiển thị mặc định trên trang /explore', true),
    ('DISCOVERY', 'DISCOVERY_MAX_PAGE_SIZE', '50', 'NUMBER', 'Số quán tối đa mỗi trang', 'Số lượng thẻ quán tối đa cho phép trong một lần request', false)
ON CONFLICT (config_key) DO UPDATE
SET config_value = EXCLUDED.config_value,
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    is_public = EXCLUDED.is_public,
    updated_at = CURRENT_TIMESTAMP;
