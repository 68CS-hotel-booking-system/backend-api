CREATE TABLE user_avatars (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    media_object_id UUID NOT NULL UNIQUE REFERENCES media_objects (id)
);

CREATE TABLE room_type_images (
    id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL,
    room_type_id UUID NOT NULL,
    media_object_id UUID NOT NULL REFERENCES media_objects (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_room_type_image_hotel
        FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_types (id, hotel_id) ON DELETE CASCADE,
    CONSTRAINT uq_room_type_media UNIQUE (room_type_id, media_object_id)
);

CREATE INDEX idx_room_type_images_media ON room_type_images (media_object_id);
