CREATE TABLE hotel_review_requests (
    id UUID PRIMARY KEY,
    hotel_id UUID NOT NULL REFERENCES hotels(id),
    snapshot JSONB NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    submitted_by UUID NOT NULL REFERENCES users(id),
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_hotel_review_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_hotel_review_decision CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL AND reason IS NULL)
        OR (status = 'APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND reason IS NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL
            AND reason IS NOT NULL AND length(btrim(reason)) > 0)
    ),
    CONSTRAINT ck_hotel_review_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_hotel_review_pending ON hotel_review_requests (hotel_id) WHERE status = 'PENDING';
CREATE INDEX idx_hotel_review_submitted ON hotel_review_requests (submitted_at DESC, id DESC);
CREATE INDEX idx_hotel_review_hotel_submitted ON hotel_review_requests (hotel_id, submitted_at DESC, id DESC);

-- Ảnh dùng UUID bền vững, không lưu URL ký có hạn. FK giữ ảnh dù Owner thay album/danh sách.
CREATE TABLE hotel_review_request_images (
    request_id UUID NOT NULL REFERENCES hotel_review_requests(id),
    media_id UUID NOT NULL REFERENCES media_objects(id),
    PRIMARY KEY (request_id, media_id)
);
CREATE INDEX idx_hotel_review_image_media ON hotel_review_request_images (media_id);

CREATE FUNCTION protect_hotel_review_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.id, NEW.hotel_id, NEW.snapshot, NEW.submitted_by, NEW.submitted_at)
        IS DISTINCT FROM ROW(OLD.id, OLD.hotel_id, OLD.snapshot, OLD.submitted_by, OLD.submitted_at)
        OR OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION 'Submitted hotel review is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_hotel_review_immutable BEFORE UPDATE ON hotel_review_requests
    FOR EACH ROW EXECUTE FUNCTION protect_hotel_review_snapshot();

-- Không cho soft-delete hoặc thay object lưu trữ của ảnh đã nằm trong hồ sơ lịch sử.
CREATE FUNCTION protect_hotel_review_media() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.status, NEW.storage_provider, NEW.bucket_name, NEW.object_key, NEW.checksum_sha256,
           NEW.hotel_id, NEW.purpose)
        IS DISTINCT FROM ROW(OLD.status, OLD.storage_provider, OLD.bucket_name, OLD.object_key,
                             OLD.checksum_sha256, OLD.hotel_id, OLD.purpose)
        AND EXISTS (SELECT 1 FROM hotel_review_request_images WHERE media_id = OLD.id) THEN
        RAISE EXCEPTION 'Media is retained by a hotel review' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_hotel_review_media BEFORE UPDATE ON media_objects
    FOR EACH ROW EXECUTE FUNCTION protect_hotel_review_media();
