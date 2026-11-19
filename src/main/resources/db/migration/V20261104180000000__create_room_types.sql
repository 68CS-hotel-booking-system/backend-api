CREATE TABLE room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    max_adults SMALLINT NOT NULL DEFAULT 2,
    max_children SMALLINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_room_types_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uq_room_types_id_hotel UNIQUE (id, hotel_id),
    CONSTRAINT ck_room_types_max_adults CHECK (max_adults >= 1),
    CONSTRAINT ck_room_types_max_children CHECK (max_children >= 0),
    CONSTRAINT ck_room_types_version CHECK (version >= 0)
);

CREATE INDEX idx_room_types_hotel_name ON room_types (hotel_id, name);
