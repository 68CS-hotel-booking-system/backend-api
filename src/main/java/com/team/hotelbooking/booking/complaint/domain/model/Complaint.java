package com.team.hotelbooking.booking.complaint.domain.model;

import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintErrorCode;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import java.time.Instant;
import java.util.UUID;

/**
 * Khiếu nại của khách gắn với một booking. Phản hồi và kết quả xử lý không tự phát sinh hoàn tiền.
 */
public record Complaint(
    UUID id,
    UUID bookingId,
    UUID customerId,
    UUID hotelId,
    Category category,
    String content,
    Status status,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static final int MAX_CONTENT_LENGTH = 2000;
  public static final int MIN_OTHER_CONTENT_LENGTH = 20;

  public enum Category {
    ROOM_CONDITION,
    SERVICE_QUALITY,
    STAFF_BEHAVIOR,
    BILLING,
    CANCELLATION_REFUND,
    OFF_PLATFORM_PAYMENT,
    OTHER
  }

  public enum Status {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    REJECTED
  }

  public Complaint handle(Status next, String response, Instant now) {
    String message = response == null ? "" : response.trim();
    if (next == null || now == null || message.isEmpty() || message.length() > MAX_CONTENT_LENGTH)
      throw ComplaintException.invalid(
          "Phản hồi phải có từ 1 đến 2000 ký tự và trạng thái hợp lệ.");
    boolean allowed =
        status == Status.OPEN && next == Status.IN_REVIEW
            || status == Status.IN_REVIEW
                && (next == Status.IN_REVIEW || next == Status.RESOLVED || next == Status.REJECTED);
    if (!allowed) throw new ComplaintException(ComplaintErrorCode.INVALID_COMPLAINT_TRANSITION);
    return new Complaint(
        id, bookingId, customerId, hotelId, category, content, next, createdAt, now, version + 1);
  }

  // Kiểm cả khi use case được gọi ngoài HTTP; nội dung đã cắt khoảng trắng đầu/cuối.
  public static Complaint submit(
      UUID id,
      UUID bookingId,
      UUID customerId,
      UUID hotelId,
      Category category,
      String content,
      Instant now) {
    if (id == null || bookingId == null || customerId == null || hotelId == null || now == null) {
      throw ComplaintException.invalid("Thiếu thông tin đơn đặt phòng hoặc người gửi khiếu nại.");
    }
    if (category == null) {
      throw ComplaintException.invalid("Loại khiếu nại không được để trống.");
    }
    String normalized = content == null ? "" : content.trim();
    if (normalized.isEmpty()) {
      throw ComplaintException.invalid("Nội dung khiếu nại không được để trống.");
    }
    if (normalized.length() > MAX_CONTENT_LENGTH) {
      throw ComplaintException.invalid(
          "Nội dung khiếu nại tối đa " + MAX_CONTENT_LENGTH + " ký tự.");
    }
    if (category == Category.OTHER && normalized.length() < MIN_OTHER_CONTENT_LENGTH) {
      throw ComplaintException.invalid(
          "Khiếu nại loại OTHER cần mô tả tối thiểu " + MIN_OTHER_CONTENT_LENGTH + " ký tự.");
    }
    return new Complaint(
        id, bookingId, customerId, hotelId, category, normalized, Status.OPEN, now, now, 0L);
  }
}
