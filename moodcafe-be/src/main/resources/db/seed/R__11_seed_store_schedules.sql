-- =============================================================
-- R__11: SEED STORE SCHEDULES FOR ALL ACTIVE STORES
-- Seeds operating hours for Monday through Sunday based on
-- each store's opening_time and closing_time.
-- =============================================================

INSERT INTO store_schedules (store_id, day_of_week, open_time, close_time, is_open)
SELECT 
    s.store_id,
    d.day_of_week,
    COALESCE(s.opening_time, '07:30:00'::TIME) AS open_time,
    COALESCE(s.closing_time, '22:30:00'::TIME) AS close_time,
    TRUE AS is_open
FROM stores s
CROSS JOIN (
    VALUES 
        ('MONDAY'),
        ('TUESDAY'),
        ('WEDNESDAY'),
        ('THURSDAY'),
        ('FRIDAY'),
        ('SATURDAY'),
        ('SUNDAY')
) AS d(day_of_week)
WHERE s.is_deleted = FALSE
ON CONFLICT (store_id, day_of_week) WHERE is_deleted = FALSE DO NOTHING;
