-- Lịch sử phản hồi công khai và người thực hiện được lưu cùng transaction cập nhật khiếu nại.
CREATE TABLE complaint_actions (
    id UUID PRIMARY KEY,
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE RESTRICT,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    actor_role VARCHAR(20) NOT NULL CHECK (actor_role IN ('ADMIN','HOTEL_OWNER')),
    from_status complaint_status__t NOT NULL,
    to_status complaint_status__t NOT NULL,
    response TEXT NOT NULL CHECK (length(btrim(response)) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL CHECK (version > 0),
    CONSTRAINT uq_complaint_action_version UNIQUE (complaint_id, version),
    CONSTRAINT ck_complaint_action_transition CHECK (
        (from_status = 'OPEN' AND to_status = 'IN_REVIEW') OR
        (from_status = 'IN_REVIEW' AND to_status IN ('IN_REVIEW','RESOLVED','REJECTED'))
    )
);
CREATE INDEX idx_complaints_hotel_created ON complaints(hotel_id, created_at DESC, id DESC);
CREATE INDEX idx_complaints_status_created ON complaints(status, created_at DESC, id DESC);
