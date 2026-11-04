CREATE TYPE amenity_category__t AS ENUM ('HOTEL', 'ROOM');

CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category amenity_category__t NOT NULL DEFAULT 'ROOM',

    CONSTRAINT uq_amenities_code UNIQUE (code)
);
