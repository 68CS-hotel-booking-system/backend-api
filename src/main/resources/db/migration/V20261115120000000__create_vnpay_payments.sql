CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd >= 5000 AND amount_vnd <= 9999999999),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND' CHECK (currency = 'VND'),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    payment_url VARCHAR(4096) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    provider_transaction_no VARCHAR(32) UNIQUE,
    response_code VARCHAR(2),
    transaction_status VARCHAR(2),
    booking_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (expires_at > created_at),
    CHECK (NOT booking_confirmed OR status = 'SUCCEEDED')
);

CREATE INDEX idx_payment_transactions_booking ON payment_transactions(booking_id);
CREATE INDEX idx_payment_transactions_customer ON payment_transactions(customer_id);
-- Một URL đang chờ cho mỗi đơn; khóa booking tuần tự hóa cả tạo mới và xử lý IPN.
CREATE UNIQUE INDEX uq_payment_transactions_pending_booking
    ON payment_transactions(booking_id) WHERE status = 'PENDING';
