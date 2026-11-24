-- Allow DB-seeded / promotional voucher campaigns to be excluded from platform P&L (Expenses & Net Income)
-- so cancelling them simply stops further claims without releasing budget into Net Income.
ALTER TABLE revenue_voucher_campaigns
    ADD COLUMN IF NOT EXISTS exclude_from_finance BOOLEAN NOT NULL DEFAULT FALSE;
