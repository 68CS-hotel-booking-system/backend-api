CREATE TABLE booking_commissions (
    booking_id UUID PRIMARY KEY REFERENCES bookings(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES hotel_contracts(id) ON DELETE RESTRICT,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    commission_rate NUMERIC(5,2) NOT NULL CHECK (commission_rate BETWEEN 0 AND 100),
    basis_amount BIGINT NOT NULL CHECK (basis_amount >= 0),
    platform_amount BIGINT NOT NULL CHECK (platform_amount >= 0 AND platform_amount <= basis_amount),
    owner_amount BIGINT NOT NULL CHECK (owner_amount >= 0),
    captured_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_booking_commissions_owner ON booking_commissions(owner_user_id);
CREATE TABLE hotel_contract_renewals (
    next_contract_id UUID PRIMARY KEY REFERENCES hotel_contracts(id),
    previous_contract_id UUID NOT NULL REFERENCES hotel_contracts(id),
    CHECK (next_contract_id <> previous_contract_id)
);
-- Không suy diễn hợp đồng cho booking cũ: thiếu snapshot được báo riêng trong báo cáo.
