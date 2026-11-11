package com.team.hotelbooking.booking.complaint.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum ComplaintErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu khiếu nại không hợp lệ."),
  BOOKING_NOT_FOUND("Không tìm thấy đơn đặt phòng."),
  BOOKING_NOT_COMPLAINABLE("Đơn đặt phòng đang chờ thanh toán, chưa thể gửi khiếu nại."),
  COMPLAINT_ALREADY_OPEN("Đơn đặt phòng này đang có khiếu nại chưa xử lý xong."),
  COMPLAINT_NOT_FOUND("Không tìm thấy khiếu nại.");

  private final String defaultMessage;

  ComplaintErrorCode(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  @Override
  public String code() {
    return name();
  }

  @Override
  public String defaultMessage() {
    return defaultMessage;
  }
}
