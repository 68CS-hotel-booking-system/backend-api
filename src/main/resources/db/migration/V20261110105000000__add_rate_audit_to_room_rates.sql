-- PR-01, PR-08: Harden room_rates with positive amount constraint and actor audit column
ALTER TABLE room_rates
    ADD COLUMN IF NOT EXISTS updated_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;

DELETE FROM room_rates WHERE amount <= 0;

ALTER TABLE room_rates
    DROP CONSTRAINT IF EXISTS ck_room_rates_amount;

ALTER TABLE room_rates
    ADD CONSTRAINT ck_room_rates_amount CHECK (amount > 0);

