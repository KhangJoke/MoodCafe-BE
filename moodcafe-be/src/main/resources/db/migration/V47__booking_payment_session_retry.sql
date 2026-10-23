-- A booking hold has one immutable deadline; individual PayOS links may end
-- earlier and be recreated without releasing the booking or voucher.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMPTZ;

UPDATE bookings
SET payment_expires_at = created_at + INTERVAL '15 minutes'
WHERE payment_expires_at IS NULL
  AND status = 'PENDING_PAYMENT';

CREATE INDEX IF NOT EXISTS idx_bookings_pending_payment_expires
    ON bookings(payment_expires_at)
    WHERE status = 'PENDING_PAYMENT' AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_payment_attempts_booking_history
    ON payment_attempts(business_type, business_id, created_at DESC)
    WHERE business_type = 'BOOKING';
