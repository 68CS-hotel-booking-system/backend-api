CREATE TYPE room_readiness__t AS ENUM ('READY', 'DIRTY');

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    floor VARCHAR(20),
    readiness room_readiness__t NOT NULL DEFAULT 'DIRTY',
    readiness_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    readiness_updated_by UUID,
    operational_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    operational_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_rooms_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT fk_rooms_room_type_hotel
        FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_types (id, hotel_id),
    CONSTRAINT fk_rooms_readiness_updated_by
        FOREIGN KEY (readiness_updated_by) REFERENCES users (id),
    CONSTRAINT uq_rooms_hotel_room_number UNIQUE (hotel_id, room_number),
    CONSTRAINT uq_rooms_id_room_type UNIQUE (id, room_type_id),
    CONSTRAINT ck_rooms_operational_period
        CHECK (operational_until IS NULL OR operational_until > operational_from),
    CONSTRAINT ck_rooms_version CHECK (version >= 0)
);

CREATE INDEX idx_rooms_type_readiness ON rooms (room_type_id, readiness);
