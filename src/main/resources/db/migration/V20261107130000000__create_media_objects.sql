CREATE TYPE media_status__t AS ENUM (
    'PENDING',
    'READY',
    'DELETED'
);

CREATE TABLE media_objects (
                             id UUID PRIMARY KEY,
                             storage_provider VARCHAR(32) NOT NULL,
                             bucket_name VARCHAR(100) NOT NULL,
                             object_key VARCHAR(512) NOT NULL,
                             original_filename VARCHAR(255) NOT NULL,
                             content_type VARCHAR(100) NOT NULL,
                             size_bytes BIGINT NOT NULL,
                             checksum_sha256 VARCHAR(64),
                             purpose VARCHAR(32) NOT NULL,
                             hotel_id UUID REFERENCES hotels(id),
                             status media_status__t NOT NULL DEFAULT 'PENDING',
                             created_by_user_id UUID NOT NULL REFERENCES users(id),
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             deleted_at TIMESTAMPTZ,

                             CONSTRAINT uq_media_objects_storage_key
                               UNIQUE (storage_provider, bucket_name, object_key),

                             CONSTRAINT ck_media_objects_size
                               CHECK (size_bytes > 0),

                             CONSTRAINT ck_media_objects_purpose
                               CHECK (
                                 purpose IN (
                                             'USER_IMAGE',
                                             'HOTEL_IMAGE',
                                             'PRIVATE_EVIDENCE'
                                   )
                                 ),

                             CONSTRAINT ck_media_objects_hotel_scope
                               CHECK (
                                 (purpose <> 'HOTEL_IMAGE' OR hotel_id IS NOT NULL)
                                   AND
                                 (purpose <> 'USER_IMAGE' OR hotel_id IS NULL)
                                 ),

                             CONSTRAINT ck_media_objects_deleted_at
                               CHECK (
                                 (status = 'DELETED' AND deleted_at IS NOT NULL)
                                   OR
                                 (status <> 'DELETED' AND deleted_at IS NULL)
                                 )
);

CREATE INDEX idx_media_objects_created_by
  ON media_objects (created_by_user_id, created_at DESC);

CREATE INDEX idx_media_objects_hotel
  ON media_objects (hotel_id);

CREATE INDEX idx_media_objects_status_created_at
  ON media_objects (status, created_at);

CREATE TABLE hotel_images (
                            hotel_id UUID NOT NULL REFERENCES hotels(id),
                            media_object_id UUID NOT NULL REFERENCES media_objects(id),
                            display_order INTEGER NOT NULL,
                            is_cover BOOLEAN NOT NULL DEFAULT false,
                            alt_text VARCHAR(255),
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                            PRIMARY KEY (hotel_id, media_object_id),

                            CONSTRAINT ck_hotel_images_display_order
                              CHECK (display_order >= 0),

                            CONSTRAINT uq_hotel_images_display_order
                              UNIQUE (hotel_id, display_order)
);

CREATE UNIQUE INDEX uq_hotel_images_cover
  ON hotel_images (hotel_id)
  WHERE is_cover = true;

CREATE INDEX idx_hotel_images_media_object
  ON hotel_images (media_object_id);
