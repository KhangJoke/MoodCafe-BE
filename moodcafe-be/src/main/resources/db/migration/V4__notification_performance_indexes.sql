-- =========================================================
-- V4: Notification query performance indexes
-- =========================================================

-- Composite index for retrieving user notifications ordered newest first
CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notifications (user_id, created_at DESC);

-- Partial index for fast unread notification counting and querying
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id)
    WHERE is_read = FALSE;
