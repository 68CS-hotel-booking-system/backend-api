-- Gia cố bảng inventory_reservations cho nghiệp vụ giữ/xác nhận/trả tồn (IV-05, IV-06, IV-07, IV-11, BK-12)
ALTER TABLE inventory_reservations
  ADD COLUMN IF NOT EXISTS booking_id UUID,
  ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS release_reason VARCHAR(50);

-- Chuẩn hóa dữ liệu cũ trước khi thêm ràng buộc CHECK
UPDATE inventory_reservations
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE status = 'HELD' AND expires_at IS NULL;

UPDATE inventory_reservations
SET released_at = now()
WHERE status = 'RELEASED' AND released_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_inv_res_held_expires_at'
  ) THEN
    ALTER TABLE inventory_reservations
      ADD CONSTRAINT ck_inv_res_held_expires_at
      CHECK (status <> 'HELD' OR expires_at IS NOT NULL);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_inv_res_released_at'
  ) THEN
    ALTER TABLE inventory_reservations
      ADD CONSTRAINT ck_inv_res_released_at
      CHECK (status <> 'RELEASED' OR released_at IS NOT NULL);
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_inv_res_booking_item_stay_date
  ON inventory_reservations (booking_item_id, stay_date)
  WHERE booking_item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inv_res_booking_id
  ON inventory_reservations (booking_id);

CREATE INDEX IF NOT EXISTS idx_inv_res_status_expires_at
  ON inventory_reservations (status, expires_at);

