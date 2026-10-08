-- ====================================================================
-- V36: REVIEW REPORT HARDENING
-- 1. Normalize legacy free-text reasons into the ReviewReportReason enum
-- 2. Soft-delete duplicate reports (same reviewer + same review)
-- 3. Enforce one active report per (review, reporter) with a partial unique index
-- ====================================================================

-- 1. Legacy free-text reasons -> OTHER (preserve original text in details)
UPDATE review_reports
SET details = CASE
                  WHEN details IS NULL OR btrim(details) = '' THEN reason
                  ELSE reason || ' - ' || details
              END,
    reason  = 'OTHER'
WHERE reason NOT IN ('SPAM', 'FAKE_REVIEW', 'INAPPROPRIATE_CONTENT', 'ABUSIVE_LANGUAGE', 'HARASSMENT', 'OTHER');

ALTER TABLE review_reports DROP CONSTRAINT IF EXISTS chk_review_reports_reason;
ALTER TABLE review_reports
    ADD CONSTRAINT chk_review_reports_reason
        CHECK (reason IN ('SPAM', 'FAKE_REVIEW', 'INAPPROPRIATE_CONTENT', 'ABUSIVE_LANGUAGE', 'HARASSMENT', 'OTHER'));

-- 2. Keep only the earliest active report per (review_id, reporter_user_id)
WITH ranked AS (
    SELECT report_id,
           ROW_NUMBER() OVER (PARTITION BY review_id, reporter_user_id ORDER BY created_at ASC, report_id ASC) AS rn
    FROM review_reports
    WHERE is_deleted = FALSE
)
UPDATE review_reports rr
SET is_deleted = TRUE,
    deleted_at = CURRENT_TIMESTAMP
FROM ranked
WHERE rr.report_id = ranked.report_id
  AND ranked.rn > 1;

-- 3. One active report per user per review
CREATE UNIQUE INDEX IF NOT EXISTS uq_review_reports_review_reporter_active
    ON review_reports (review_id, reporter_user_id)
    WHERE is_deleted = FALSE;
