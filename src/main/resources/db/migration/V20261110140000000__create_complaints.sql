-- BK-21 (tuần 1): khách gửi khiếu nại dạng chữ gắn với booking của mình.
-- Chưa có đính kèm bằng chứng (complaint_attachments để tuần 2) và chưa có lịch sử xử lý của admin.
-- Thêm loại khiếu nại sau bằng ALTER TYPE complaint_category__t ADD VALUE.
CREATE TYPE complaint_category__t AS ENUM (
    'ROOM_CONDITION',
    'SERVICE_QUALITY',
    'STAFF_BEHAVIOR',
    'BILLING',
    'CANCELLATION_REFUND',
    'OFF_PLATFORM_PAYMENT',
    'OTHER'
);

CREATE TYPE complaint_status__t AS ENUM (
    'OPEN',
    'IN_REVIEW',
    'RESOLVED',
    'REJECTED'
);

-- customer_id và hotel_id chép từ booking lúc gửi để lọc theo khách/khách sạn không cần join;
-- FK ghép (booking_id, hotel_id) bảo đảm hotel_id khớp booking.
CREATE TABLE complaints (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    hotel_id UUID NOT NULL,
    category complaint_category__t NOT NULL,
    content TEXT NOT NULL,
    status complaint_status__t NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_complaints_booking_hotel
        FOREIGN KEY (booking_id, hotel_id) REFERENCES bookings (id, hotel_id) ON DELETE RESTRICT,
    CONSTRAINT fk_complaints_customer
        FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_complaints_content CHECK (length(btrim(content)) BETWEEN 1 AND 2000),
    CONSTRAINT ck_complaints_version CHECK (version >= 0)
);

-- Chống gửi lặp: mỗi booking tối đa một khiếu nại đang xử lý (OPEN/IN_REVIEW);
-- khiếu nại đã RESOLVED/REJECTED không chặn khiếu nại mới.
CREATE UNIQUE INDEX uq_complaints_open_per_booking
    ON complaints (booking_id)
    WHERE status IN ('OPEN', 'IN_REVIEW');

CREATE INDEX idx_complaints_customer_created ON complaints (customer_id, created_at DESC, id);
