CREATE TABLE guest_reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    hotel_id UUID NOT NULL REFERENCES hotels(id),
    booking_code VARCHAR(20) NOT NULL,
    author_display_name VARCHAR(110) NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content VARCHAR(2000) NOT NULL CHECK (length(btrim(content)) > 0),
    created_at TIMESTAMPTZ NOT NULL,
    reply VARCHAR(2000), replied_by UUID REFERENCES users(id), replied_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT ck_guest_review_reply CHECK (
      (reply IS NULL AND replied_by IS NULL AND replied_at IS NULL) OR
      (reply IS NOT NULL AND length(btrim(reply)) > 0 AND replied_by IS NOT NULL AND replied_at IS NOT NULL))
);
CREATE INDEX idx_guest_reviews_hotel_created ON guest_reviews(hotel_id,created_at DESC,id DESC);
CREATE INDEX idx_guest_reviews_hotel_rating ON guest_reviews(hotel_id,rating);
CREATE FUNCTION protect_guest_review_content() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.booking_id,NEW.customer_id,NEW.hotel_id,NEW.booking_code,NEW.author_display_name,NEW.rating,NEW.content,NEW.created_at)
       IS DISTINCT FROM ROW(OLD.booking_id,OLD.customer_id,OLD.hotel_id,OLD.booking_code,OLD.author_display_name,OLD.rating,OLD.content,OLD.created_at) THEN
        RAISE EXCEPTION 'Guest review content is immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_guest_review_immutable BEFORE UPDATE ON guest_reviews
FOR EACH ROW EXECUTE FUNCTION protect_guest_review_content();
