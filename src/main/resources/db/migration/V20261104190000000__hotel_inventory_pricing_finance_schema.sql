-- Enums for TV5 (Inventory, Pricing, Finance)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'room_block_status__t') THEN
        CREATE TYPE room_block_status__t AS ENUM ('ACTIVE', 'RELEASED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'inventory_reservation_status__t') THEN
        CREATE TYPE inventory_reservation_status__t AS ENUM ('HELD', 'CONFIRMED', 'RELEASED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'hotel_member_role__t') THEN
        CREATE TYPE hotel_member_role__t AS ENUM ('HOTEL_MANAGER', 'RECEPTIONIST');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'commission_basis__t') THEN
        CREATE TYPE commission_basis__t AS ENUM ('BEFORE_DISCOUNT', 'AFTER_DISCOUNT', 'AFTER_HOTEL_DISCOUNT');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'hotel_contract_status__t') THEN
        CREATE TYPE hotel_contract_status__t AS ENUM ('DRAFT', 'ACTIVE', 'EXPIRED', 'TERMINATED');
    END IF;
END $$;

-- Extend room_types for pricing & occupancy
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS max_occupancy SMALLINT NOT NULL DEFAULT 2;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS base_price BIGINT NOT NULL DEFAULT 0;

-- 0. hotel_members
CREATE TABLE IF NOT EXISTS hotel_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role hotel_member_role__t NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_hotel_members_hotel_user_role UNIQUE (hotel_id, user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_hotel_members_user_id ON hotel_members(user_id);

-- 1. room_rates (PR01)
CREATE TABLE IF NOT EXISTS room_rates (
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    rate_date DATE NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_type_id, rate_date),
    CONSTRAINT ck_room_rates_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_room_rates_date ON room_rates(rate_date);

-- 2. room_inventory (IV01)
CREATE TABLE IF NOT EXISTS room_inventory (
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    inventory_date DATE NOT NULL,
    total_inventory SMALLINT NOT NULL DEFAULT 0,
    reserved_inventory SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (room_type_id, inventory_date),
    CONSTRAINT ck_room_inventory_positive CHECK (total_inventory >= 0 AND reserved_inventory >= 0)
);

-- 3. room_blocks (IV01)
CREATE TABLE IF NOT EXISTS room_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(id) ON DELETE SET NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255),
    status room_block_status__t NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    released_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at TIMESTAMPTZ,
    CONSTRAINT ck_room_blocks_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_room_blocks_room_type_dates ON room_blocks(room_type_id, start_date, end_date);

-- 4. inventory_reservations (IV01)
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_item_id UUID,
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    stay_date DATE NOT NULL,
    quantity SMALLINT NOT NULL DEFAULT 1,
    status inventory_reservation_status__t NOT NULL DEFAULT 'HELD',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at TIMESTAMPTZ,
    CONSTRAINT ck_inv_res_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_inv_res_room_type_date ON inventory_reservations(room_type_id, stay_date, status);

-- 5. hotel_contracts (FN01, FN02)
CREATE TABLE IF NOT EXISTS hotel_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_code VARCHAR(100) NOT NULL,
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    commission_rate NUMERIC(5,2) NOT NULL,
    commission_basis commission_basis__t NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status hotel_contract_status__t NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_hotel_contracts_code UNIQUE (contract_code),
    CONSTRAINT ck_hotel_contracts_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_hotel_contracts_rate CHECK (commission_rate >= 0.00 AND commission_rate <= 100.00)
);

CREATE INDEX IF NOT EXISTS idx_hotel_contracts_hotel ON hotel_contracts(hotel_id, status);

-- 6. hotel_financial_access (FN00, FN16)
CREATE TABLE IF NOT EXISTS hotel_financial_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_granted BOOLEAN NOT NULL DEFAULT FALSE,
    access_scope VARCHAR(50) NOT NULL DEFAULT 'FINANCE_READ',
    reason TEXT,
    granted_by_user_id UUID NOT NULL REFERENCES users(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_hotel_financial_access_hotel_user UNIQUE (hotel_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_hotel_financial_access_user ON hotel_financial_access(user_id);

CREATE TABLE IF NOT EXISTS hotel_financial_access_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financial_access_id UUID NOT NULL REFERENCES hotel_financial_access(id) ON DELETE CASCADE,
    hotel_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_granted BOOLEAN NOT NULL,
    access_scope VARCHAR(50),
    reason TEXT,
    version BIGINT NOT NULL,
    changed_by_user_id UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_financial_access_history ON hotel_financial_access_history(financial_access_id, version);

