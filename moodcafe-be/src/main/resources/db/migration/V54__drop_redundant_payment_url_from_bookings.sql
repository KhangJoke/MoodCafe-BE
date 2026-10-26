-- =========================================================================
-- V54: DROP REDUNDANT PAYMENT_URL FROM BOOKINGS
-- Reverts back to Khang's clean design: PayOS payment checkout URLs and QRs
-- are cached dynamically in Redis with 15m TTL (booking:payos:<id>),
-- and redirect URLs are managed strictly via environment variables (.env).
-- =========================================================================

ALTER TABLE bookings DROP COLUMN IF EXISTS payment_url;
