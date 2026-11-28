CREATE TABLE hotel_vouchers (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), code VARCHAR(30) NOT NULL, name VARCHAR(100) NOT NULL,
 discount_type VARCHAR(10) NOT NULL CHECK(discount_type IN ('PERCENT','FIXED')),
 discount_value BIGINT NOT NULL CHECK(discount_value BETWEEN 1 AND 1000000000000),
 max_discount_amount BIGINT CHECK(max_discount_amount BETWEEN 1 AND 1000000000000),
 min_order_amount BIGINT NOT NULL CHECK(min_order_amount BETWEEN 0 AND 1000000000000),
 starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL CHECK(ends_at>starts_at),
 usage_limit INTEGER NOT NULL CHECK(usage_limit BETWEEN 1 AND 1000000), enabled BOOLEAN NOT NULL,
 created_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_hotel_voucher_code UNIQUE(hotel_id,code),
 CHECK(code ~ '^[A-Z0-9][A-Z0-9_-]{2,29}$'), CHECK(length(btrim(name))>0),
 CHECK((discount_type='PERCENT' AND discount_value<=99) OR (discount_type='FIXED' AND max_discount_amount IS NULL))
);
CREATE INDEX idx_voucher_hotel_created ON hotel_vouchers(hotel_id,created_at DESC,id DESC);
CREATE TABLE voucher_redemptions (
 booking_id UUID PRIMARY KEY REFERENCES bookings(id), voucher_id UUID NOT NULL REFERENCES hotel_vouchers(id),
 code VARCHAR(30) NOT NULL, discount_type VARCHAR(10) NOT NULL, discount_value BIGINT NOT NULL,
 max_discount_amount BIGINT, min_order_amount BIGINT NOT NULL,
 room_amount BIGINT NOT NULL CHECK(room_amount>0), discount_amount BIGINT NOT NULL CHECK(discount_amount>0 AND discount_amount<room_amount),
 expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(10) NOT NULL CHECK(status IN ('RESERVED','USED','RELEASED'))
);
CREATE INDEX idx_voucher_redemptions_usage ON voucher_redemptions(voucher_id,status,expires_at);
CREATE FUNCTION protect_voucher_redemption_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.booking_id,NEW.voucher_id,NEW.code,NEW.discount_type,NEW.discount_value,NEW.max_discount_amount,NEW.min_order_amount,NEW.room_amount,NEW.discount_amount,NEW.expires_at,NEW.created_at)
 IS DISTINCT FROM ROW(OLD.booking_id,OLD.voucher_id,OLD.code,OLD.discount_type,OLD.discount_value,OLD.max_discount_amount,OLD.min_order_amount,OLD.room_amount,OLD.discount_amount,OLD.expires_at,OLD.created_at) THEN
 RAISE EXCEPTION 'Voucher snapshot is immutable';
 END IF;
 IF NEW.status<>OLD.status AND NOT (OLD.status='RESERVED' AND NEW.status IN ('USED','RELEASED')) THEN
 RAISE EXCEPTION 'Invalid voucher redemption transition';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER trg_voucher_redemption_snapshot BEFORE UPDATE ON voucher_redemptions FOR EACH ROW EXECUTE FUNCTION protect_voucher_redemption_snapshot();
