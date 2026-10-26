-- =========================================================================
-- V51: BOOKING CHECKOUT OVERHAUL & PHYSICAL TABLE MAPPING
-- Adds physical table mapping, checkout requested timestamp, and early check-in tracking.
-- =========================================================================

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS physical_table_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS check_out_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_early_check_in BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_bookings_status_table_num ON bookings(store_id, status, physical_table_number);
CREATE INDEX IF NOT EXISTS idx_bookings_store_status_time ON bookings(store_id, status, start_time, end_time);
