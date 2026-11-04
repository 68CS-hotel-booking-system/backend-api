-- Giá trị ACTIVE dùng cho khách sạn đã công khai (HT04). Không dùng giá trị mới trong cùng migration
-- vì PostgreSQL chỉ cho phép sử dụng enum value sau khi transaction thêm nó đã commit.
ALTER TYPE hotel_status__t ADD VALUE IF NOT EXISTS 'ACTIVE';
