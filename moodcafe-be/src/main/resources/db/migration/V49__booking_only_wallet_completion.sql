-- Do not drop historical tables or rewrite previously applied migrations.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM reservations WHERE status IN ('PENDING_PAYMENT','PAID_CONFIRMED','CHECKED_IN','TABLE_EXPIRED'))
 OR EXISTS (SELECT 1 FROM customer_vouchers WHERE locked_reservation_id IS NOT NULL AND status='LOCKED') THEN
  RAISE EXCEPTION 'Active legacy reservations require explicit reconciliation before disabling the duplicate module';
 END IF;
END $$;
ALTER TABLE voucher_templates ADD COLUMN min_gross_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(min_gross_amount>=0);
ALTER TABLE voucher_templates ADD COLUMN valid_days INTEGER NOT NULL DEFAULT 30 CHECK(valid_days BETWEEN 1 AND 3650);
ALTER TABLE customer_vouchers ADD COLUMN min_gross_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(min_gross_amount>=0);
ALTER TABLE merchant_wallets ADD COLUMN total_withdrawn NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK(total_withdrawn>=0);
UPDATE merchant_wallets w SET total_withdrawn=(SELECT coalesce(sum(amount),0) FROM merchant_withdrawals p WHERE p.store_id=w.store_id AND p.status='PAID');
CREATE TABLE payout_booking_allocations (
 withdrawal_id UUID NOT NULL REFERENCES merchant_withdrawals(withdrawal_id),
 source_transaction_id UUID NOT NULL REFERENCES merchant_wallet_transactions(transaction_id),
 booking_id UUID REFERENCES bookings(booking_id),
 source_reference_type VARCHAR(50),
 source_reference_id VARCHAR(100),
 booking_code VARCHAR(100) NOT NULL,
 amount NUMERIC(12,2) NOT NULL CHECK(amount>0),
 PRIMARY KEY(withdrawal_id,source_transaction_id)
);
CREATE INDEX idx_payout_booking_source ON payout_booking_allocations(booking_id);
