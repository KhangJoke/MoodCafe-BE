-- V45 adds zero defaults; retain the known legacy amount instead of presenting
-- existing paid bookings as free. Historical commission/owner balances cannot
-- be reconstructed from min_spend_amount and are deliberately not invented.
UPDATE bookings
SET gross_amount = GREATEST(min_spend_amount, 0),
    customer_paid_amount = GREATEST(min_spend_amount, 0),
    actual_paid_amount = CASE
        WHEN status IN ('PAID_CONFIRMED', 'CHECKED_IN', 'COMPLETED',
                        'TABLE_EXPIRED', 'CLAIMED_LATE', 'EXPIRED_NO_SHOW')
        THEN GREATEST(min_spend_amount, 0)
        ELSE actual_paid_amount
    END,
    commission_rate_snapshot = 0
WHERE quote_id IS NULL
  AND gross_amount = 0 AND customer_paid_amount = 0
  AND voucher_id IS NULL;

-- Deadline snapshots for old records use the existing store configuration.
-- Vietnam has no DST. Match the application's overnight closing convention.
UPDATE bookings b
SET free_cancel_deadline = COALESCE(b.free_cancel_deadline,
        b.start_time - make_interval(hours => COALESCE(c.free_cancel_hours, 2))),
    grace_deadline = COALESCE(b.grace_deadline,
        b.start_time + make_interval(mins => COALESCE(c.grace_period_mins, 30))),
    service_closing_deadline = COALESCE(b.service_closing_deadline,
        (((b.start_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
          + CASE WHEN s.closing_time < s.opening_time THEN 1 ELSE 0 END)
          + COALESCE(s.closing_time, TIME '22:00')) AT TIME ZONE 'Asia/Ho_Chi_Minh')
FROM stores s LEFT JOIN store_booking_configs c ON c.store_id = s.store_id
WHERE b.store_id = s.store_id AND b.quote_id IS NULL;

-- V45's BOOKING default also covered historic reservation/subscription attempts.
UPDATE payment_attempts a SET business_type = 'RESERVATION'
WHERE a.purpose = 'BOOKING'
  AND EXISTS (SELECT 1 FROM reservations r WHERE r.reservation_id::text = a.business_id)
  AND NOT EXISTS (SELECT 1 FROM bookings b WHERE b.booking_id::text = a.business_id);
UPDATE payment_attempts SET business_type = purpose WHERE purpose <> 'BOOKING';

ALTER TABLE customer_vouchers ADD CONSTRAINT ck_voucher_single_booking_source
    CHECK (num_nonnulls(locked_booking_id, locked_reservation_id) <= 1);
ALTER TABLE reservation_loyalty_rewards ADD CONSTRAINT ck_loyalty_single_booking_source
    CHECK (num_nonnulls(booking_id, reservation_id) = 1);
ALTER TABLE reviews ADD CONSTRAINT ck_review_single_booking_source
    CHECK (num_nonnulls(booking_id, reservation_id) <= 1);

-- One active order per quote; retry returns the original intent/booking.
CREATE UNIQUE INDEX uq_bookings_quote_active ON bookings(quote_id)
    WHERE quote_id IS NOT NULL AND is_deleted = FALSE;

-- V44's actor index is non-unique. Distinct refreshed quotes must still share
-- exactly one business intent. Conflicting historic rows stop migration for
-- explicit reconciliation; never delete financial attempts to make it pass.
CREATE UNIQUE INDEX uq_payment_attempts_actor_purpose_intent
    ON payment_attempts(actor_id, purpose, intent_key)
    WHERE actor_id IS NOT NULL AND intent_key IS NOT NULL;
