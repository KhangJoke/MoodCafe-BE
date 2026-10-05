-- =========================================================
-- V35: ADD ACTION_URL COLUMN TO NOTIFICATIONS TABLE FOR DEEP LINKING
-- =========================================================

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS action_url VARCHAR(500);
