-- Revenue-funded voucher campaigns & source tracking for customer vouchers
CREATE TABLE IF NOT EXISTS revenue_voucher_campaigns (
    campaign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    distribution_type VARCHAR(30) NOT NULL CHECK (distribution_type IN ('DIRECT_GRANT', 'CLAIMABLE_POOL')),
    funding_source VARCHAR(30) NOT NULL DEFAULT 'ALL' CHECK (funding_source IN ('ALL', 'SUBSCRIPTION_PAYMENT', 'BOOKING_COMMISSION')),
    face_value_vnd NUMERIC(12, 2) NOT NULL CHECK (face_value_vnd > 0),
    min_gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (min_gross_amount >= 0),
    valid_days INT NOT NULL DEFAULT 30 CHECK (valid_days > 0),
    total_quantity INT NOT NULL CHECK (total_quantity > 0),
    issued_quantity INT NOT NULL DEFAULT 0 CHECK (issued_quantity >= 0),
    max_claims_per_user INT NOT NULL DEFAULT 1 CHECK (max_claims_per_user > 0),
    allocated_budget_vnd NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (allocated_budget_vnd >= 0),
    released_budget_vnd NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (released_budget_vnd >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rev_voucher_campaigns_status ON revenue_voucher_campaigns(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rev_voucher_campaigns_source ON revenue_voucher_campaigns(funding_source, status);
CREATE INDEX IF NOT EXISTS idx_rev_voucher_campaigns_dist ON revenue_voucher_campaigns(distribution_type, status);

ALTER TABLE customer_vouchers
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) NOT NULL DEFAULT 'MOODPOINT_REDEEM',
    ADD COLUMN IF NOT EXISTS campaign_id UUID REFERENCES revenue_voucher_campaigns(campaign_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS title VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_customer_vouchers_source_type ON customer_vouchers(source_type);
CREATE INDEX IF NOT EXISTS idx_customer_vouchers_campaign ON customer_vouchers(campaign_id, user_id);

