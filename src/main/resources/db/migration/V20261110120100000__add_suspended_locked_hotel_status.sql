-- Vòng đời bán của khách sạn (HT-05, HT-06, HT-09):
--   SUSPENDED: Owner/HOTEL_MANAGER tạm ngừng bán, tự mở lại được.
--   LOCKED: quản trị hệ thống khóa bán, chỉ quản trị hệ thống gỡ được.
-- Dữ liệu cũ giữ nguyên (DRAFT/ACTIVE). Không dùng giá trị mới trong cùng migration vì PostgreSQL
-- chỉ cho dùng enum value sau khi transaction thêm nó đã commit.
ALTER TYPE hotel_status__t ADD VALUE IF NOT EXISTS 'SUSPENDED';
ALTER TYPE hotel_status__t ADD VALUE IF NOT EXISTS 'LOCKED';
