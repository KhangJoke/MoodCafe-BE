-- =========================================================================
-- V41: SINGLE MOODPOINTS, WALLET, VOUCHER, RESERVATION & SETTLEMENT SCHEMA
-- Supports 1 MoodPoint = 1 VND, single customer loyalty/refund wallet,
-- voucher catalog, store merchant wallets (3 balances), and reservation lifecycle.
-- =========================================================================

-- 1. Commission Policies
CREATE TABLE IF NOT EXISTS commission_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(store_id) ON DELETE CASCADE, -- NULL means global platform policy
    commission_rate NUMERIC(5, 4) NOT NULL DEFAULT 0.1500, -- e.g. 0.15 = 15%
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_commission_policies_store ON commission_policies(store_id, is_active);

-- Seed default global commission policy (15%)
INSERT INTO commission_policies (store_id, commission_rate, is_active)
VALUES (NULL, 0.1500, TRUE);

-- 2. Table Categories & Store Tables
-- Reuse V40's existing table_categories: max_capacity and BIGINT min_spend_amount.
-- Do not introduce a second category schema or replace existing booking data.
CREATE INDEX IF NOT EXISTS idx_table_categories_store ON table_categories(store_id, is_active, is_deleted);

CREATE TABLE IF NOT EXISTS store_tables (
    table_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES table_categories(category_id) ON DELETE RESTRICT,
    table_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_store_tables_store ON store_tables(store_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_store_tables_category ON store_tables(category_id, is_deleted);

-- 3. Customer Wallets & Point Lots (Single MoodPoints Balance)
CREATE TABLE IF NOT EXISTS customer_wallets (
    wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    points_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customer_wallets_user ON customer_wallets(user_id);

CREATE TABLE IF NOT EXISTS point_lots (
    lot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    source_type VARCHAR(30) NOT NULL, -- REFUND, REWARD
    source_id VARCHAR(100) NOT NULL, -- reservation_id, review_id, promo_id, etc.
    issued_points BIGINT NOT NULL,
    remaining_points BIGINT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE, -- NULL for REFUND (never expires); 365 days for REWARD
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_point_lots_user_active ON point_lots(user_id, source_type, remaining_points);
CREATE INDEX IF NOT EXISTS idx_point_lots_expiry ON point_lots(expires_at) WHERE remaining_points > 0;

CREATE TABLE IF NOT EXISTS point_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_key VARCHAR(150) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    amount BIGINT NOT NULL, -- positive for credits, negative for debits
    balance_after BIGINT NOT NULL,
    tx_type VARCHAR(50) NOT NULL, -- REFUND_CREDIT, REWARD_CREDIT, REDEEM_DEBIT, REWARD_REVERSAL, EXPIRED_DEBIT
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_point_transactions_user ON point_transactions(user_id, created_at DESC);

-- 4. Voucher Templates & Customer Vouchers
CREATE TABLE IF NOT EXISTS voucher_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    face_value_vnd NUMERIC(12, 2) NOT NULL, -- 10000, 20000, 50000, 100000, 200000
    point_cost BIGINT NOT NULL, -- exactly equals face_value_vnd (1 point = 1 VND)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed standard fixed denominations according to Policy P2
INSERT INTO voucher_templates (title, description, face_value_vnd, point_cost, display_order)
VALUES 
    ('Voucher Giảm 10.000đ', 'Đổi 10.000 MoodPoints để giảm 10.000đ cho đơn đặt bàn tiếp theo', 10000.00, 10000, 1),
    ('Voucher Giảm 20.000đ', 'Đổi 20.000 MoodPoints để giảm 20.000đ cho đơn đặt bàn tiếp theo', 20000.00, 20000, 2),
    ('Voucher Giảm 50.000đ', 'Đổi 50.000 MoodPoints để giảm 50.000đ cho đơn đặt bàn tiếp theo', 50000.00, 50000, 3),
    ('Voucher Giảm 100.000đ', 'Đổi 100.000 MoodPoints để giảm 100.000đ cho đơn đặt bàn tiếp theo', 100000.00, 100000, 4),
    ('Voucher Giảm 200.000đ', 'Đổi 200.000 MoodPoints để giảm 200.000đ cho đơn đặt bàn tiếp theo', 200000.00, 200000, 5)
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS customer_vouchers (
    voucher_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    template_id UUID REFERENCES voucher_templates(template_id) ON DELETE SET NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    face_value_vnd NUMERIC(12, 2) NOT NULL,
    point_cost BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, LOCKED, USED, EXPIRED, REVOKED
    locked_reservation_id UUID,
    hold_until TIMESTAMP WITH TIME ZONE,
    used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customer_vouchers_user ON customer_vouchers(user_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_vouchers_lock ON customer_vouchers(locked_reservation_id, status);

CREATE TABLE IF NOT EXISTS voucher_point_allocations (
    allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id UUID NOT NULL REFERENCES customer_vouchers(voucher_id) ON DELETE CASCADE,
    lot_id UUID NOT NULL REFERENCES point_lots(lot_id) ON DELETE CASCADE,
    allocated_points BIGINT NOT NULL,
    funding_type VARCHAR(30) NOT NULL, -- REFUND, REWARD
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_voucher_allocations_voucher ON voucher_point_allocations(voucher_id);

-- 5. Reservations (Lifecycle, 5 Financial Amounts, Funding Breakdown)
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    category_id UUID REFERENCES table_categories(category_id) ON DELETE SET NULL,
    table_id UUID REFERENCES store_tables(table_id) ON DELETE SET NULL,
    booking_time TIMESTAMP WITH TIME ZONE NOT NULL,
    guest_count INT NOT NULL DEFAULT 2,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT',
    -- Status: PENDING_PAYMENT, PAID_CONFIRMED, CHECKED_IN, COMPLETED, TABLE_EXPIRED, CLAIMED_LATE, EXPIRED_NO_SHOW, CANCELLED_REFUNDED, PAYMENT_FAILED

    -- 5 Financial Snapshot Fields
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    voucher_discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    customer_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    commission_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    merchant_net_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    -- Voucher & Funding breakdown
    voucher_id UUID REFERENCES customer_vouchers(voucher_id) ON DELETE SET NULL,
    voucher_refund_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    voucher_reward_funded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    commission_rate_snapshot NUMERIC(5, 4) NOT NULL DEFAULT 0.1500,

    -- PayOS Payment details
    payos_order_code BIGINT UNIQUE,
    payos_payment_link_id VARCHAR(150),
    payment_url VARCHAR(500),

    -- Verification & Lifecycle tracking
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    claimed_late_time TIMESTAMP WITH TIME ZONE,
    grace_deadline TIMESTAMP WITH TIME ZONE,
    service_closing_deadline TIMESTAMP WITH TIME ZONE,

    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reservations_store ON reservations(store_id, status, booking_time);
CREATE INDEX IF NOT EXISTS idx_reservations_user ON reservations(user_id, status);
CREATE INDEX IF NOT EXISTS idx_reservations_payos_code ON reservations(payos_order_code);

-- 6. Merchant Wallets (Per Store: Pending, Available, Locked in VND)
CREATE TABLE IF NOT EXISTS merchant_wallets (
    wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE REFERENCES stores(store_id) ON DELETE CASCADE,
    pending_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    locked_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_merchant_wallets_store ON merchant_wallets(store_id);

CREATE TABLE IF NOT EXISTS merchant_wallet_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES merchant_wallets(wallet_id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    balance_type VARCHAR(30) NOT NULL, -- PENDING, AVAILABLE, LOCKED
    tx_type VARCHAR(50) NOT NULL, -- HOLD_PENDING, RELEASE_TO_AVAILABLE, LOCK_FOR_WITHDRAWAL, WITHDRAWAL_PAID, WITHDRAWAL_CANCELLED, REVERSAL
    reference_type VARCHAR(50), -- RESERVATION, WITHDRAWAL
    reference_id VARCHAR(100),
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_merchant_tx_wallet ON merchant_wallet_transactions(wallet_id, created_at DESC);

-- 7. Store Bank Accounts & Merchant Withdrawals
CREATE TABLE IF NOT EXISTS store_bank_accounts (
    bank_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(50), -- e.g. VCB, MB, TCB
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_store_bank_accounts_store ON store_bank_accounts(store_id);

CREATE TABLE IF NOT EXISTS merchant_withdrawals (
    withdrawal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED, PROCESSING, PAID, REJECTED, CANCELLED
    admin_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    admin_notes TEXT,
    transaction_reference VARCHAR(150),
    evidence_image_url VARCHAR(500),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_merchant_withdrawals_store ON merchant_withdrawals(store_id, status);
CREATE INDEX IF NOT EXISTS idx_merchant_withdrawals_status ON merchant_withdrawals(status, created_at DESC);

-- 8. Loyalty Rewards Tracking (Dedup per reservation)
CREATE TABLE IF NOT EXISTS reservation_loyalty_rewards (
    reward_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    reward_type VARCHAR(50) NOT NULL, -- SPEND, ON_TIME_CHECK_IN, SELF_CHECK_OUT, VERIFIED_REVIEW
    points_awarded BIGINT NOT NULL,
    lot_id UUID REFERENCES point_lots(lot_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reservation_reward_type UNIQUE (reservation_id, reward_type)
);
CREATE INDEX IF NOT EXISTS idx_loyalty_rewards_user ON reservation_loyalty_rewards(user_id);
