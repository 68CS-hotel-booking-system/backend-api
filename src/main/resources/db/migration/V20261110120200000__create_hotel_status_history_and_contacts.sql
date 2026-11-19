-- HT-03: thông tin liên hệ là điều kiện mở bán. Khách sạn cũ chưa có liên hệ (NULL) nên phải bổ
-- sung trước khi publish/resume; dữ liệu cũ không bị đổi trạng thái.
ALTER TABLE hotels
    ADD COLUMN contact_phone VARCHAR(20),
    ADD COLUMN contact_email VARCHAR(255);

-- Lịch sử đổi trạng thái bán. Không CASCADE khi xóa hotel: HT-08 giữ lịch sử thay vì xóa.
CREATE TABLE hotel_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL,
    from_status hotel_status__t NOT NULL,
    to_status hotel_status__t NOT NULL,
    reason VARCHAR(500),
    changed_by_user_id UUID NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_hotel_status_history_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT fk_hotel_status_history_actor FOREIGN KEY (changed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_hotel_status_history_changed CHECK (from_status <> to_status)
);

CREATE INDEX idx_hotel_status_history_hotel_time ON hotel_status_history (hotel_id, changed_at);
