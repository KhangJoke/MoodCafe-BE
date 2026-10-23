-- Financial history only: never credit/debit merchant wallets during backfill.
CREATE TABLE commission_policy_scopes(scope_key VARCHAR(40) PRIMARY KEY);
ALTER TABLE commission_policies ADD COLUMN supersedes_policy_id UUID REFERENCES commission_policies(policy_id);
ALTER TABLE commission_policies ADD COLUMN changed_by UUID REFERENCES users(user_id);
CREATE TABLE commission_policy_audit (
 audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 policy_id UUID NOT NULL REFERENCES commission_policies(policy_id),
 action VARCHAR(24) NOT NULL,
 actor_id UUID REFERENCES users(user_id),
 commission_rate NUMERIC(5,4) NOT NULL,
 effective_from TIMESTAMPTZ NOT NULL,
 effective_to TIMESTAMPTZ,
 is_active BOOLEAN NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_policy_audit_time ON commission_policy_audit(policy_id,occurred_at);
ALTER TABLE booking_quotes ADD COLUMN commission_policy_id UUID REFERENCES commission_policies(policy_id);
ALTER TABLE bookings ADD COLUMN commission_policy_id UUID REFERENCES commission_policies(policy_id);

-- Resolve legacy overlapping policies deterministically, preserving every old row and booking snapshot.
WITH ordered AS (
 SELECT policy_id, lead(effective_from) OVER (PARTITION BY store_id ORDER BY effective_from, created_at, policy_id) AS next_start
 FROM commission_policies WHERE is_active = TRUE
)
UPDATE commission_policies p SET effective_to = o.next_start
FROM ordered o WHERE p.policy_id = o.policy_id AND o.next_start IS NOT NULL
AND (p.effective_to IS NULL OR p.effective_to > o.next_start);

ALTER TABLE commission_policies ADD CONSTRAINT chk_commission_rate CHECK (commission_rate >= 0 AND commission_rate <= 1);
CREATE TABLE booking_commission_entries (
 entry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 booking_id UUID NOT NULL REFERENCES bookings(booking_id),
 store_id UUID NOT NULL REFERENCES stores(store_id),
 event_type VARCHAR(16) NOT NULL CHECK (event_type IN ('HOLD','SETTLE','REVERSE')),
 amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
 currency VARCHAR(3) NOT NULL DEFAULT 'VND',
 business_key VARCHAR(120) NOT NULL UNIQUE,
 source VARCHAR(40) NOT NULL,
 original_entry_id UUID REFERENCES booking_commission_entries(entry_id),
 occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE(booking_id, event_type)
);
CREATE INDEX idx_commission_store_time ON booking_commission_entries(store_id, occurred_at);
CREATE INDEX idx_commission_booking ON booking_commission_entries(booking_id);
CREATE INDEX idx_bookings_store_paid ON bookings(store_id, paid_at DESC, booking_id);

-- Only wallet entries explicitly bound to BOOKING are proof for this ledger.
INSERT INTO booking_commission_entries(booking_id,store_id,event_type,amount,business_key,source,occurred_at)
SELECT b.booking_id,b.store_id,'HOLD',b.commission_amount,'BOOKING:'||b.booking_id||':HOLD','WALLET_BACKFILL',min(t.created_at)
FROM bookings b JOIN merchant_wallet_transactions t ON t.reference_type='BOOKING' AND t.reference_id=b.booking_id::text
JOIN merchant_wallets w ON w.wallet_id=t.wallet_id AND w.store_id=b.store_id
WHERE t.tx_type='HOLD_PENDING' AND abs(t.amount-b.merchant_net_amount)<0.01
GROUP BY b.booking_id ON CONFLICT DO NOTHING;

-- Zero net (100% commission) has no merchant wallet movement; paid timestamp is required.
INSERT INTO booking_commission_entries(booking_id,store_id,event_type,amount,business_key,source,occurred_at)
SELECT b.booking_id,b.store_id,'HOLD',b.commission_amount,'BOOKING:'||b.booking_id||':HOLD','PAYMENT_BACKFILL',b.paid_at
FROM bookings b WHERE b.paid_at IS NOT NULL AND b.merchant_net_amount=0
AND b.status IN ('PAID_CONFIRMED','CHECKED_IN','TABLE_EXPIRED','COMPLETED','CLAIMED_LATE','EXPIRED_NO_SHOW','CANCELLED_REFUNDED')
ON CONFLICT DO NOTHING;

INSERT INTO booking_commission_entries(booking_id,store_id,event_type,amount,business_key,source,original_entry_id,occurred_at)
SELECT b.booking_id,b.store_id,CASE WHEN t.tx_type='REVERSAL' THEN 'REVERSE' ELSE 'SETTLE' END,b.commission_amount,
 'BOOKING:'||b.booking_id||':'||CASE WHEN t.tx_type='REVERSAL' THEN 'REVERSE' ELSE 'SETTLE' END,
 'WALLET_BACKFILL',h.entry_id,min(t.created_at)
FROM bookings b JOIN booking_commission_entries h ON h.booking_id=b.booking_id AND h.event_type='HOLD'
JOIN merchant_wallet_transactions t ON t.reference_type='BOOKING' AND t.reference_id=b.booking_id::text
JOIN merchant_wallets w ON w.wallet_id=t.wallet_id AND w.store_id=b.store_id
WHERE t.tx_type IN ('RELEASE_TO_AVAILABLE','REVERSAL') AND abs(abs(t.amount)-b.merchant_net_amount)<0.01
GROUP BY b.booking_id,t.tx_type,h.entry_id ON CONFLICT DO NOTHING;

INSERT INTO booking_commission_entries(booking_id,store_id,event_type,amount,business_key,source,original_entry_id,occurred_at)
SELECT b.booking_id,b.store_id,CASE WHEN b.status='CANCELLED_REFUNDED' THEN 'REVERSE' ELSE 'SETTLE' END,b.commission_amount,
 'BOOKING:'||b.booking_id||':'||CASE WHEN b.status='CANCELLED_REFUNDED' THEN 'REVERSE' ELSE 'SETTLE' END,
 'PAYMENT_BACKFILL',h.entry_id,COALESCE(b.completed_at,b.claimed_late_at,b.cancelled_at,b.updated_at)
FROM bookings b JOIN booking_commission_entries h ON h.booking_id=b.booking_id AND h.event_type='HOLD'
WHERE b.merchant_net_amount=0 AND b.status IN ('COMPLETED','CLAIMED_LATE','EXPIRED_NO_SHOW','CANCELLED_REFUNDED')
ON CONFLICT DO NOTHING;
