-- V39: Durable Payment Attempts, OrderCode Sequence, Payout Reference Uniqueness & Voucher Redemption Idempotency

-- 1. Sequence for global orderCode across subscriptions and bookings (safe for multi-instance)
CREATE SEQUENCE IF NOT EXISTS payment_order_code_seq START WITH 2000000001 INCREMENT BY 1;

-- 2. Durable payment attempts table
CREATE TABLE IF NOT EXISTS payment_attempts (
    attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purpose VARCHAR(50) NOT NULL, -- 'BOOKING' or 'SUBSCRIPTION'
    business_id VARCHAR(100) NOT NULL, -- reservationId or subscription paymentId
    order_code BIGINT NOT NULL UNIQUE,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED, EXPIRED, UNKNOWN
    gateway_payment_link_id VARCHAR(150),
    checkout_url TEXT,
    qr_code_url TEXT,
    intent_key VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_business ON payment_attempts(purpose, business_id);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_order_code ON payment_attempts(order_code);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_intent ON payment_attempts(intent_key);

-- 3. Unique transaction reference for PAID merchant withdrawals (anti double-payout)
CREATE UNIQUE INDEX IF NOT EXISTS idx_merchant_withdrawals_tx_ref 
    ON merchant_withdrawals (trim(lower(transaction_reference))) 
    WHERE transaction_reference IS NOT NULL AND status = 'PAID';

-- 4. Resolution audit columns for withdrawals
ALTER TABLE merchant_withdrawals
    ADD COLUMN IF NOT EXISTS resolution_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS resolved_by UUID,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolution_notes TEXT;

-- 5. Voucher redemption intents for idempotent redemption
CREATE TABLE IF NOT EXISTS voucher_redemptions (
    redemption_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    template_id UUID NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    voucher_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_voucher_redemptions_user_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_voucher_redemptions_user_key ON voucher_redemptions(user_id, idempotency_key);

-- 6. Link reviews to reservations for verified booking review rewards
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS reservation_id UUID;

CREATE INDEX IF NOT EXISTS idx_reviews_reservation_id ON reviews(reservation_id);

