CREATE TABLE booking_email_notifications (
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    is_reminder BOOLEAN NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (booking_id, is_reminder)
);

CREATE INDEX idx_booking_email_notifications_due
    ON booking_email_notifications (due_at, booking_id, is_reminder);

INSERT INTO booking_email_notifications (booking_id, is_reminder, due_at)
SELECT id, TRUE,
       (check_in + expected_check_in_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' - INTERVAL '24 hours'
FROM bookings
WHERE status = 'CONFIRMED'
  AND (check_in + expected_check_in_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' > CURRENT_TIMESTAMP;
