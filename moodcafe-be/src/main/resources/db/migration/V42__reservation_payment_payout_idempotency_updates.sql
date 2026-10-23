-- V38__reservation_payment_payout_idempotency_updates.sql
-- Idempotency, claim locks, bank binding, and payment integrity for single MoodPoints & reservations

-- 1. Merchant Withdrawals enhancements
ALTER TABLE merchant_withdrawals
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS claimed_by UUID,
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bank_code VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uq_merchant_withdrawals_idempotency
    ON merchant_withdrawals(store_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 2. Reservations payment attempt & state integrity
ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS payment_purpose VARCHAR(50) DEFAULT 'BOOKING',
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS actual_paid_amount NUMERIC(12, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Store Bank Accounts soft delete
ALTER TABLE store_bank_accounts
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
