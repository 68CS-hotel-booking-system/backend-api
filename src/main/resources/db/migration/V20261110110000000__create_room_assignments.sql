-- BK-15..18, BK-08..10, BK-14, IV-10..11: Physical room assignments and stay operations state
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'room_assignment_status__t') THEN
        CREATE TYPE room_assignment_status__t AS ENUM ('ASSIGNED', 'CHECKED_IN', 'CHECKED_OUT', 'RELEASED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_stay_status__t') THEN
        CREATE TYPE booking_stay_status__t AS ENUM ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'NO_SHOW');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS room_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    booking_id UUID NOT NULL,
    booking_item_id UUID NOT NULL,
    room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    room_number_snapshot VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status room_assignment_status__t NOT NULL DEFAULT 'ASSIGNED',
    assigned_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_in_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    checked_in_at TIMESTAMPTZ,
    checked_out_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    checked_out_at TIMESTAMPTZ,
    released_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    released_at TIMESTAMPTZ,
    release_reason VARCHAR(255),
    CONSTRAINT ck_room_assignments_dates CHECK (end_date > start_date)
);

CREATE INDEX IF NOT EXISTS idx_room_assignments_booking_id
    ON room_assignments(booking_id, status);

CREATE INDEX IF NOT EXISTS idx_room_assignments_room_dates
    ON room_assignments(room_id, start_date, end_date)
    WHERE status IN ('ASSIGNED', 'CHECKED_IN');

CREATE INDEX IF NOT EXISTS idx_room_assignments_hotel_dates
    ON room_assignments(hotel_id, start_date, end_date)
    WHERE status IN ('ASSIGNED', 'CHECKED_IN');

CREATE TABLE IF NOT EXISTS booking_stay_operations (
    booking_id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    stay_status booking_stay_status__t NOT NULL DEFAULT 'CONFIRMED',
    late_arrival_expected_at TIMESTAMPTZ,
    late_arrival_note VARCHAR(500),
    late_arrival_recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    late_arrival_recorded_at TIMESTAMPTZ,
    checked_in_by UUID REFERENCES users(id) ON DELETE SET NULL,
    checked_in_at TIMESTAMPTZ,
    checked_out_by UUID REFERENCES users(id) ON DELETE SET NULL,
    checked_out_at TIMESTAMPTZ,
    no_show_reason VARCHAR(500),
    no_show_marked_by UUID REFERENCES users(id) ON DELETE SET NULL,
    no_show_marked_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

