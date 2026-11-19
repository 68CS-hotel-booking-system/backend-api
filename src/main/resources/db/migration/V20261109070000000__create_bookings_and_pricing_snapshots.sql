DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status__t') THEN
        CREATE TYPE booking_status__t AS ENUM (
            'PENDING_PAYMENT',
            'CONFIRMED',
            'CHECKED_IN',
            'CHECKED_OUT',
            'CANCELLED',
            'EXPIRED',
            'NO_SHOW'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(20) NOT NULL,
    customer_id UUID NOT NULL,
    hotel_id UUID NOT NULL,
    booker_name_snapshot VARCHAR(100) NOT NULL,
    booker_email_snapshot VARCHAR(255) NOT NULL,
    booker_phone_snapshot VARCHAR(20) NOT NULL,
    guest_name_snapshot VARCHAR(100) NOT NULL,
    guest_email_snapshot VARCHAR(255) NOT NULL,
    guest_phone_snapshot VARCHAR(20) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    days SMALLINT NOT NULL,
    nights SMALLINT NOT NULL,
    check_in_time_snapshot TIME NOT NULL DEFAULT '14:00:00',
    check_out_time_snapshot TIME NOT NULL DEFAULT '12:00:00',
    expected_check_in_time TIME NOT NULL DEFAULT '14:00:00',
    expected_check_out_time TIME NOT NULL DEFAULT '12:00:00',
    adults SMALLINT NOT NULL DEFAULT 1,
    children SMALLINT NOT NULL DEFAULT 0,
    status booking_status__t NOT NULL DEFAULT 'PENDING_PAYMENT',
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    room_subtotal_amount BIGINT NOT NULL DEFAULT 0,
    early_check_in_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    late_check_out_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    extra_guest_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    surcharge_amount BIGINT NOT NULL DEFAULT 0,
    subtotal_amount BIGINT NOT NULL DEFAULT 0,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_bookings_booking_code UNIQUE (booking_code),
    CONSTRAINT uq_bookings_id_hotel UNIQUE (id, hotel_id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE RESTRICT,
    CONSTRAINT ck_bookings_dates CHECK (check_out > check_in AND nights >= 1 AND days = nights + 1),
    CONSTRAINT ck_bookings_guests CHECK (adults >= 1 AND children >= 0),
    CONSTRAINT ck_bookings_amounts CHECK (
        room_subtotal_amount >= 0
        AND early_check_in_surcharge_amount >= 0
        AND late_check_out_surcharge_amount >= 0
        AND extra_guest_surcharge_amount >= 0
        AND surcharge_amount >= 0
        AND subtotal_amount = room_subtotal_amount + surcharge_amount
        AND discount_amount >= 0
        AND discount_amount <= subtotal_amount
        AND total_amount = subtotal_amount - discount_amount
    ),
    CONSTRAINT ck_bookings_version CHECK (version >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bookings_customer_id ON bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_hotel_id ON bookings (hotel_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status_expires_at ON bookings (status, expires_at);

CREATE TABLE IF NOT EXISTS booking_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    hotel_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    room_type_name_snapshot VARCHAR(100) NOT NULL,
    quantity SMALLINT NOT NULL DEFAULT 1,
    adults SMALLINT NOT NULL DEFAULT 1,
    children SMALLINT NOT NULL DEFAULT 0,
    extra_guests SMALLINT NOT NULL DEFAULT 0,
    room_subtotal_amount BIGINT NOT NULL DEFAULT 0,
    early_check_in_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    late_check_out_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    extra_guest_surcharge_amount BIGINT NOT NULL DEFAULT 0,
    surcharge_amount BIGINT NOT NULL DEFAULT 0,
    subtotal_amount BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_booking_items_booking_hotel FOREIGN KEY (booking_id, hotel_id) REFERENCES bookings (id, hotel_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_room_type_hotel FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_types (id, hotel_id) ON DELETE RESTRICT,
    CONSTRAINT uq_booking_items_id_room_type UNIQUE (id, room_type_id),
    CONSTRAINT ck_booking_items_quantity CHECK (quantity > 0 AND adults >= quantity AND children >= 0 AND extra_guests >= 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_items_booking_id ON booking_items (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_room_type_id ON booking_items (room_type_id);

CREATE TABLE IF NOT EXISTS booking_night_prices (
    booking_item_id UUID NOT NULL,
    stay_date DATE NOT NULL,
    unit_price BIGINT NOT NULL,

    PRIMARY KEY (booking_item_id, stay_date),
    CONSTRAINT fk_booking_night_prices_booking_item FOREIGN KEY (booking_item_id) REFERENCES booking_items (id) ON DELETE CASCADE,
    CONSTRAINT ck_booking_night_prices_unit_price CHECK (unit_price >= 0)
);

CREATE TABLE IF NOT EXISTS booking_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    from_status booking_status__t,
    to_status booking_status__t NOT NULL,
    changed_by_user_id UUID,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_status_history_actor FOREIGN KEY (changed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_status_history_booking_time ON booking_status_history (booking_id, created_at);
