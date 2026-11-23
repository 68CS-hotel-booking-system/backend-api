ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bookings_customer_idempotency_key
    ON bookings (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
