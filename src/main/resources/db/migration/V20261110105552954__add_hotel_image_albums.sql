CREATE TABLE hotel_image_albums (
                                  id UUID PRIMARY KEY,
                                  hotel_id UUID NOT NULL REFERENCES hotels(id),
                                  name VARCHAR(100) NOT NULL,
                                  description TEXT,
                                  sort_order INTEGER NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                  CONSTRAINT ck_hotel_image_albums_name
                                    CHECK (length(btrim(name)) > 0),

                                  CONSTRAINT ck_hotel_image_albums_order
                                    CHECK (sort_order >= 0),

                                  CONSTRAINT uq_hotel_image_albums_scope
                                    UNIQUE (hotel_id, id),

                                  CONSTRAINT uq_hotel_image_albums_order
                                    UNIQUE (hotel_id, sort_order)
                                      DEFERRABLE INITIALLY DEFERRED
);

ALTER TABLE hotel_images
  ADD COLUMN album_id UUID;

ALTER TABLE hotel_images
  ADD CONSTRAINT fk_hotel_images_album
    FOREIGN KEY (hotel_id, album_id)
      REFERENCES hotel_image_albums (hotel_id, id);

CREATE INDEX idx_hotel_images_album
  ON hotel_images (hotel_id, album_id);
