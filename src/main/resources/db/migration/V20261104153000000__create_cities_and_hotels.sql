CREATE TYPE hotel_status__t AS ENUM ('DRAFT');

CREATE TABLE cities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(150) NOT NULL,

    CONSTRAINT uq_cities_slug UNIQUE (slug)
);

CREATE INDEX idx_cities_country_name ON cities (country_code, name);

CREATE TABLE hotels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    city_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    star_rating SMALLINT,
    check_in_time TIME NOT NULL DEFAULT '14:00:00',
    check_out_time TIME NOT NULL DEFAULT '12:00:00',
    status hotel_status__t NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_hotels_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_hotels_city FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT uq_hotels_slug UNIQUE (slug),
    CONSTRAINT ck_hotels_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_hotels_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_hotels_star_rating CHECK (star_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_hotels_version CHECK (version >= 0)
);

CREATE INDEX idx_hotels_owner_user_id ON hotels (owner_user_id);
CREATE INDEX idx_hotels_city_id ON hotels (city_id);
CREATE INDEX idx_hotels_status ON hotels (status);
