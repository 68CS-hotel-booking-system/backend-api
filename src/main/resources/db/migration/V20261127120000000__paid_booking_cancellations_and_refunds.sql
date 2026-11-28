CREATE TABLE payment_refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payment_transactions(id),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    hotel_id UUID NOT NULL REFERENCES hotels(id),
    booking_code VARCHAR(20) NOT NULL,
    booker_name VARCHAR(100), booker_email VARCHAR(255), booker_phone VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0), currency VARCHAR(3) NOT NULL CHECK (currency = 'VND'),
    reason VARCHAR(40) NOT NULL CHECK (reason IN ('CUSTOMER_CANCELLATION','UNCONFIRMED_PAYMENT')),
    customer_note VARCHAR(1000),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','PROCESSING','COMPLETED')),
    settlement_reference VARCHAR(100), note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT ck_refund_completed CHECK (
      (status='COMPLETED' AND completed_at IS NOT NULL AND settlement_reference IS NOT NULL AND length(btrim(settlement_reference)) > 0)
      OR (status<>'COMPLETED' AND completed_at IS NULL AND settlement_reference IS NULL))
);
CREATE UNIQUE INDEX uq_payment_refunds_reference ON payment_refunds(lower(settlement_reference)) WHERE settlement_reference IS NOT NULL;
CREATE INDEX idx_payment_refunds_customer ON payment_refunds(customer_id,created_at DESC,id DESC);
CREATE INDEX idx_payment_refunds_hotel ON payment_refunds(hotel_id,created_at DESC,id DESC);
CREATE INDEX idx_payment_refunds_booking ON payment_refunds(booking_id);
-- Những khoản thu trước khi có sổ hoàn tiền nhưng không xác nhận được đơn vẫn phải được xử lý.
INSERT INTO payment_refunds(id,payment_id,booking_id,customer_id,hotel_id,booking_code,
    booker_name,booker_email,booker_phone,amount,currency,reason,status,created_at,updated_at)
SELECT gen_random_uuid(),p.id,b.id,b.customer_id,b.hotel_id,b.booking_code,
    b.booker_name_snapshot,b.booker_email_snapshot,b.booker_phone_snapshot,p.amount_vnd,p.currency,
    'UNCONFIRMED_PAYMENT','PENDING',now(),now()
FROM payment_transactions p JOIN bookings b ON b.id=p.booking_id
WHERE p.status='SUCCEEDED' AND NOT p.booking_confirmed;
CREATE TABLE payment_refund_actions (
    id UUID PRIMARY KEY, refund_id UUID NOT NULL REFERENCES payment_refunds(id),
    actor_id UUID NOT NULL REFERENCES users(id), from_status VARCHAR(16) NOT NULL, to_status VARCHAR(16) NOT NULL,
    note VARCHAR(1000) NOT NULL CHECK (length(btrim(note)) > 0), settlement_reference VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL,
    CONSTRAINT uq_refund_action_version UNIQUE(refund_id,version),
    CONSTRAINT ck_refund_action_transition CHECK ((from_status='PENDING' AND to_status='PROCESSING') OR (from_status='PROCESSING' AND to_status='COMPLETED'))
);
