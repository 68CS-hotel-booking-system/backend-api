-- Chỉ module Payment ghi giao dịch xác thực và điều khoản áp dụng khi xác nhận đơn.
CREATE TABLE verified_booking_payments (
    booking_id UUID PRIMARY KEY REFERENCES bookings(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL CHECK (btrim(provider) <> ''),
    transaction_reference VARCHAR(255) NOT NULL CHECK (btrim(transaction_reference) <> ''),
    amount BIGINT NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL,
    contract_id UUID NOT NULL REFERENCES hotel_contracts(id),
    commission_rate NUMERIC(5,2) NOT NULL CHECK (commission_rate BETWEEN 0 AND 100),
    UNIQUE (provider, transaction_reference)
);

ALTER TABLE booking_status_history ADD COLUMN cancelled_by_hotel BOOLEAN;

CREATE TABLE booking_cancellation_settlements (
    booking_id UUID PRIMARY KEY REFERENCES verified_booking_payments(booking_id) ON DELETE CASCADE,
    cancellation_fee_amount BIGINT NOT NULL CHECK (cancellation_fee_amount >= 0),
    refund_amount BIGINT NOT NULL CHECK (refund_amount >= 0),
    commission_amount BIGINT NOT NULL CHECK (commission_amount BETWEEN 0 AND cancellation_fee_amount),
    refund_status VARCHAR(12) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK ((refund_amount = 0 AND refund_status = 'NOT_REQUIRED') OR
           (refund_amount > 0 AND refund_status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')))
);
