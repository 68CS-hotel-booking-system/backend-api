-- Khóa (id, category) để bảng liên kết dùng FK kép: DB tự chặn gắn nhầm nhóm tiện ích.
-- id đã là PK nên dữ liệu amenities hiện có luôn thỏa ràng buộc này.
ALTER TABLE amenities ADD CONSTRAINT uq_amenities_id_category UNIQUE (id, category);

-- Tiện ích của khách sạn chỉ nhận amenity category HOTEL.
CREATE TABLE hotel_amenities (
    hotel_id UUID NOT NULL,
    amenity_id UUID NOT NULL,
    -- Cột cố định, ứng dụng không ghi; chỉ để FK kép trỏ đúng (amenity_id, 'HOTEL').
    amenity_category amenity_category__t NOT NULL DEFAULT 'HOTEL',

    CONSTRAINT pk_hotel_amenities PRIMARY KEY (hotel_id, amenity_id),
    CONSTRAINT fk_hotel_amenities_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (id) ON DELETE CASCADE,
    CONSTRAINT fk_hotel_amenities_amenity FOREIGN KEY (amenity_id, amenity_category)
        REFERENCES amenities (id, category),
    CONSTRAINT ck_hotel_amenities_category CHECK (amenity_category = 'HOTEL')
);

CREATE INDEX idx_hotel_amenities_amenity_id ON hotel_amenities (amenity_id);

-- Tiện ích của loại phòng chỉ nhận amenity category ROOM.
CREATE TABLE room_type_amenities (
    room_type_id UUID NOT NULL,
    amenity_id UUID NOT NULL,
    -- Cột cố định, ứng dụng không ghi; chỉ để FK kép trỏ đúng (amenity_id, 'ROOM').
    amenity_category amenity_category__t NOT NULL DEFAULT 'ROOM',

    CONSTRAINT pk_room_type_amenities PRIMARY KEY (room_type_id, amenity_id),
    CONSTRAINT fk_room_type_amenities_room_type FOREIGN KEY (room_type_id)
        REFERENCES room_types (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_type_amenities_amenity FOREIGN KEY (amenity_id, amenity_category)
        REFERENCES amenities (id, category),
    CONSTRAINT ck_room_type_amenities_category CHECK (amenity_category = 'ROOM')
);

CREATE INDEX idx_room_type_amenities_amenity_id ON room_type_amenities (amenity_id);
