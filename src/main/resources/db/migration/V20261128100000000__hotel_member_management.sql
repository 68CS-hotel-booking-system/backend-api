ALTER TABLE hotel_members
  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE hotel_members
  ADD CONSTRAINT ck_hotel_members_version CHECK (version >= 0);

CREATE INDEX IF NOT EXISTS idx_hotel_members_hotel_active
  ON hotel_members(hotel_id, is_active);
