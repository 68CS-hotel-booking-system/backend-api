CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID REFERENCES hotels (id) ON DELETE RESTRICT,
    code VARCHAR(50) NOT NULL UNIQUE CHECK (code <> '' AND code = upper(btrim(code))),
    name VARCHAR(150) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    percentage_off NUMERIC(5, 2),
    fixed_amount BIGINT,
    max_discount_amount BIGINT CHECK (max_discount_amount > 0),
    min_booking_amount BIGINT NOT NULL DEFAULT 0 CHECK (min_booking_amount >= 0),
    usage_limit INTEGER CHECK (usage_limit > 0),
    per_user_limit INTEGER NOT NULL DEFAULT 1 CHECK (per_user_limit > 0),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'EXPIRED', 'ARCHIVED')),
    funding_source VARCHAR(10) NOT NULL CHECK (funding_source IN ('PLATFORM', 'HOTEL')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (funding_source <> 'HOTEL' OR hotel_id IS NOT NULL),
    CHECK ((discount_type = 'PERCENTAGE' AND percentage_off IS NOT NULL
        AND percentage_off > 0 AND percentage_off <= 100 AND fixed_amount IS NULL)
        OR (discount_type = 'FIXED_AMOUNT' AND fixed_amount IS NOT NULL
        AND fixed_amount > 0 AND percentage_off IS NULL))
);

CREATE INDEX idx_promotions_hotel_id ON promotions (hotel_id);

CREATE TABLE promotion_redemptions (
    booking_id UUID PRIMARY KEY REFERENCES bookings (id) ON DELETE RESTRICT,
    promotion_id UUID NOT NULL REFERENCES promotions (id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    code_snapshot VARCHAR(50) NOT NULL,
    funding_source VARCHAR(10) NOT NULL CHECK (funding_source IN ('PLATFORM', 'HOTEL')),
    discount_amount BIGINT NOT NULL CHECK (discount_amount > 0),
    status VARCHAR(10) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED', 'CANCELLED', 'REFUNDED')),
    redeemed_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ,
    CHECK ((status = 'APPLIED') = (released_at IS NULL))
);

CREATE INDEX idx_promotion_redemptions_active ON promotion_redemptions (promotion_id, user_id)
    WHERE status = 'APPLIED';

CREATE TABLE booking_creation_requests (
    customer_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    request_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    PRIMARY KEY (customer_id, request_key)
);
