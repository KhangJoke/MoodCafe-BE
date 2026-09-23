-- ====================================================================
-- V28: MERCHANT FLOW SCHEMA UPDATES
-- 1. Store approval updates: reject_reason, allow_resubmit
-- 2. Store tags approval updates: allow_resubmit
-- 3. Review merchant reply: merchant_reply, reply_at
-- 4. Review fraud reports: review_reports table
-- ====================================================================

-- 1. Stores: Add reject_reason and allow_resubmit
ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS reject_reason TEXT,
    ADD COLUMN IF NOT EXISTS allow_resubmit BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Store tags: Add allow_resubmit (reject_reason already exists from V1/V11)
ALTER TABLE store_tags
    ADD COLUMN IF NOT EXISTS allow_resubmit BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Reviews: Add merchant_reply and reply_at
ALTER TABLE reviews
    ADD COLUMN IF NOT EXISTS merchant_reply TEXT,
    ADD COLUMN IF NOT EXISTS reply_at TIMESTAMPTZ;

-- 4. Review Reports table (Chống review bẩn / Fraud report)
CREATE TABLE IF NOT EXISTS review_reports (
    report_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL REFERENCES reviews(review_id) ON DELETE CASCADE,
    reporter_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_review_reports_review_id ON review_reports (review_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_store_id ON review_reports (store_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_status ON review_reports (status);
