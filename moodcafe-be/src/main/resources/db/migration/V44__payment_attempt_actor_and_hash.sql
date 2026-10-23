-- V40: Add actor_id and payload_hash to payment_attempts for scoped idempotency
ALTER TABLE payment_attempts
    ADD COLUMN IF NOT EXISTS actor_id UUID,
    ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);

-- Ensure index on actor_id, purpose, intent_key
CREATE INDEX IF NOT EXISTS idx_payment_attempts_actor_purpose_intent
    ON payment_attempts(actor_id, purpose, intent_key);
