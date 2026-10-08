-- =========================================================================
-- V37: STORE SCHEDULES SCHEMA
-- Supports daily operating schedules (opening / closing hours per day of week)
-- for cafe merchants and store detail display.
-- =========================================================================

CREATE TABLE IF NOT EXISTS store_schedules (
    schedule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME,
    close_time TIME,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_schedules_store FOREIGN KEY (store_id) REFERENCES stores(store_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_store_schedules_store_id ON store_schedules(store_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_store_day_of_week_active
    ON store_schedules(store_id, day_of_week)
    WHERE is_deleted = FALSE;
