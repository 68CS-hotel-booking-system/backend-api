-- Gia cố bảng room_blocks cho nghiệp vụ quản lý lịch chặn bảo trì phòng vật lý (BL-01..12)
ALTER TABLE room_blocks
  ADD COLUMN IF NOT EXISTS updated_by_user_id UUID REFERENCES users(id),
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS release_reason VARCHAR(255);

UPDATE room_blocks
SET released_at = now()
WHERE status = 'RELEASED' AND released_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_room_blocks_released_at'
  ) THEN
    ALTER TABLE room_blocks
      ADD CONSTRAINT ck_room_blocks_released_at
      CHECK (status <> 'RELEASED' OR released_at IS NOT NULL);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_room_blocks_room_status_dates
  ON room_blocks (room_id, status, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_room_blocks_hotel_dates
  ON room_blocks (hotel_id, start_date, end_date);

