-- =========================================================================
-- V45: CANONICAL BOOKING FINANCIAL SNAPSHOT, QUOTES & UNIFIED BINDINGS
-- Integrates 5 financial immutable amounts, quotes, voucher locks, loyalty
-- rewards and review bindings directly into canonical bookings schema.
-- =========================================================================

-- 1. Booking Quotes
CREATE TABLE IF NOT EXISTS booking_quotes (
    quote_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES table_categories(category_id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    guest_count INT NOT NULL DEFAULT 2,
    voucher_id UUID REFERENCES customer_vouchers(voucher_id) ON DELETE SET NULL,
    selection_fingerprint VARCHAR(64) NOT NULL,
    pricing_version INT NOT NULL DEFAULT 1,
    gross_amount NUMERIC(12, 2) NOT NULL,
    voucher_discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    customer_paid_amount NUMERIC(12, 2) NOT NULL,
    commission_rate NUMERIC(5, 4) NOT NULL,
    commission_amount NUMERIC(12, 2) NOT NULL,
    merchant_net_amount NUMERIC(12, 2) NOT NULL,
    voucher_refund_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    voucher_reward_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    ineligibility_reason TEXT,
    free_cancel_deadline TIMESTAMPTZ NOT NULL,
    grace_deadline TIMESTAMPTZ NOT NULL,
    service_closing_deadline TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_booking_quotes_user ON booking_quotes(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_quotes_store_category ON booking_quotes(store_id, category_id);

-- 2. Enhance Bookings table with immutable financial snapshot & deadlines
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS guest_count INT NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS quote_id UUID REFERENCES booking_quotes(quote_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS voucher_discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS customer_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS merchant_net_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS voucher_id UUID REFERENCES customer_vouchers(voucher_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS voucher_refund_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS voucher_reward_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS commission_rate_snapshot NUMERIC(5, 4) NOT NULL DEFAULT 0.1500,
    ADD COLUMN IF NOT EXISTS actual_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS payment_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS free_cancel_deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS grace_deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS service_closing_deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Voucher lock binding for canonical bookings
ALTER TABLE customer_vouchers
    ADD COLUMN IF NOT EXISTS locked_booking_id UUID REFERENCES bookings(booking_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_customer_vouchers_booking_lock 
    ON customer_vouchers(locked_booking_id, status)
    WHERE locked_booking_id IS NOT NULL;

-- 4. Loyalty rewards binding for canonical bookings
ALTER TABLE reservation_loyalty_rewards
    ALTER COLUMN reservation_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS booking_id UUID REFERENCES bookings(booking_id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_loyalty_reward 
    ON reservation_loyalty_rewards(booking_id, reward_type) 
    WHERE booking_id IS NOT NULL;

-- 5. Review binding for verified booking reviews
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS booking_id UUID REFERENCES bookings(booking_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_booking_id 
    ON reviews(booking_id) 
    WHERE booking_id IS NOT NULL;

-- 6. Payment attempts business_type classifier
ALTER TABLE payment_attempts
    ADD COLUMN IF NOT EXISTS business_type VARCHAR(50) DEFAULT 'BOOKING';
