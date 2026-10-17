-- =========================================================
-- V40: CREATE BOOKING MODULE SCHEMA
-- =========================================================

-- 1. store_booking_configs
CREATE TABLE IF NOT EXISTS store_booking_configs (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores (store_id) ON DELETE CASCADE,
    is_booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_time_limit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sitting_duration_mins INTEGER NOT NULL DEFAULT 120,
    cleanup_buffer_mins INTEGER NOT NULL DEFAULT 15,
    grace_period_mins INTEGER NOT NULL DEFAULT 30,
    min_advance_mins INTEGER NOT NULL DEFAULT 30,
    free_cancel_hours INTEGER NOT NULL DEFAULT 2,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_store_booking_configs_store_id ON store_booking_configs (store_id);

-- 2. table_categories
CREATE TABLE IF NOT EXISTS table_categories (
    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores (store_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    max_capacity INTEGER NOT NULL DEFAULT 2,
    app_quota INTEGER NOT NULL DEFAULT 0,
    min_spend_amount BIGINT NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_table_categories_store_id ON table_categories (store_id);
CREATE INDEX IF NOT EXISTS idx_table_categories_active ON table_categories (is_active) WHERE is_deleted = FALSE;

-- 3. bookings
CREATE TABLE IF NOT EXISTS bookings (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(32) NOT NULL,
    store_id UUID NOT NULL REFERENCES stores (store_id),
    category_id UUID NOT NULL REFERENCES table_categories (category_id),
    user_id UUID NOT NULL REFERENCES users (user_id),
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50),
    customer_email VARCHAR(255),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    min_spend_amount BIGINT NOT NULL DEFAULT 0,
    payment_order_code BIGINT,
    qr_code_token VARCHAR(255),
    checked_in_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    claimed_late_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,
    refund_processed BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bookings_code_active ON bookings (booking_code) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_bookings_store_id ON bookings (store_id);
CREATE INDEX IF NOT EXISTS idx_bookings_category_id ON bookings (category_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_start_time ON bookings (status, start_time);
CREATE INDEX IF NOT EXISTS idx_bookings_status_created_at ON bookings (status, created_at);
CREATE INDEX IF NOT EXISTS idx_bookings_payment_order_code ON bookings (payment_order_code);
