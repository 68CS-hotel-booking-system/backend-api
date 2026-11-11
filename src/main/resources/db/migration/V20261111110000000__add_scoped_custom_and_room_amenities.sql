-- Phòng vật lý có tiện ích riêng, không sửa danh sách kế thừa từ hạng phòng.
ALTER TABLE rooms ADD CONSTRAINT uq_rooms_id_hotel UNIQUE (id, hotel_id);

CREATE TABLE room_amenities (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL,
    amenity_category amenity_category__t NOT NULL DEFAULT 'ROOM',
    CONSTRAINT pk_room_amenities PRIMARY KEY (room_id, amenity_id),
    CONSTRAINT fk_room_amenities_catalog FOREIGN KEY (amenity_id, amenity_category)
        REFERENCES amenities(id, category),
    CONSTRAINT ck_room_amenities_category CHECK (amenity_category = 'ROOM')
);
CREATE INDEX idx_room_amenities_amenity ON room_amenities(amenity_id);

-- room_id NULL: tiện ích riêng của hotel; có room_id: chỉ của phòng vật lý đó.
-- attached=false giữ tiện ích đã tháo để chủ cơ sở có thể gắn lại.
CREATE TABLE custom_amenities (
    id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    room_id UUID,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    attached BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_custom_amenities_room_hotel FOREIGN KEY (room_id, hotel_id)
        REFERENCES rooms(id, hotel_id) ON DELETE CASCADE,
    CONSTRAINT ck_custom_amenities_name CHECK (btrim(name) <> '' AND btrim(normalized_name) <> ''),
    CONSTRAINT ck_custom_amenities_version CHECK (version >= 0)
);
CREATE UNIQUE INDEX uq_custom_amenities_hotel_name
    ON custom_amenities(hotel_id, normalized_name) WHERE room_id IS NULL;
CREATE UNIQUE INDEX uq_custom_amenities_room_name
    ON custom_amenities(room_id, normalized_name) WHERE room_id IS NOT NULL;
CREATE INDEX idx_custom_amenities_scope ON custom_amenities(hotel_id, room_id);
