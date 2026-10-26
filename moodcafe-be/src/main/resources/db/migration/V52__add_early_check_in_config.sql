-- =========================================================================
-- V52: ADD EARLY CHECK-IN CONFIGURATION TO STORE_BOOKING_CONFIGS
-- Allows stores to enable/disable early check-in and customize lead time.
-- =========================================================================

ALTER TABLE store_booking_configs
    ADD COLUMN IF NOT EXISTS is_early_check_in_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS max_early_check_in_mins INTEGER NOT NULL DEFAULT 60;
