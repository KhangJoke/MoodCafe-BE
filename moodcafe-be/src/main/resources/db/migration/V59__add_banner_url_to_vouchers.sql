-- =========================================================================
-- V59: ADD BANNER PICTURE URL TO VOUCHER CAMPAIGNS, TEMPLATES & CUSTOMER VOUCHERS
-- Enables homepage promotion thumbnails / carousels using voucher banner images
-- =========================================================================

ALTER TABLE revenue_voucher_campaigns
    ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);

ALTER TABLE voucher_templates
    ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);

ALTER TABLE customer_vouchers
    ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
