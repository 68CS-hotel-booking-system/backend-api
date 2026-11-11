-- PM-16: không cho hai hợp đồng ACTIVE của cùng khách sạn có khoảng hiệu lực chồng nhau.
-- Kiểm ở service vẫn có thể bị hai giao dịch chạy song song vượt qua; ràng buộc này là bảo đảm cuối.
-- Khoảng hiệu lực là [starts_at, ends_at): hợp đồng sau bắt đầu đúng lúc hợp đồng trước kết thúc
-- không bị coi là chồng.
-- btree_gist cần cho phép so sánh "=" trên uuid trong chỉ mục GiST; là extension trusted từ PG13.
-- Nếu DB đã có dữ liệu ACTIVE chồng nhau, migration này dừng với lỗi 23P01: phải xử lý dữ liệu trước.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE hotel_contracts
    ADD CONSTRAINT ex_hotel_contracts_active_period
        EXCLUDE USING gist (
            hotel_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        ) WHERE (status = 'ACTIVE');
