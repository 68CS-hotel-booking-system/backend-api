package com.team.hotelbooking.booking.review.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum GuestReviewErrorCode implements ErrorCode {
  VALIDATION_FAILED("Thông tin đánh giá không hợp lệ."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác với đánh giá này."),
  BOOKING_NOT_FOUND("Không tìm thấy đơn đặt phòng của bạn."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  GUEST_REVIEW_NOT_FOUND("Không tìm thấy đánh giá."),
  BOOKING_NOT_REVIEWABLE("Chỉ được đánh giá sau khi đã trả phòng."),
  GUEST_REVIEW_ALREADY_EXISTS("Đơn đặt phòng này đã được đánh giá."),
  GUEST_REVIEW_VERSION_CONFLICT("Đánh giá đã được cập nhật. Vui lòng tải lại trước khi phản hồi.");

  private final String message;

  GuestReviewErrorCode(String message) {
    this.message = message;
  }

  public String code() {
    return name();
  }

  public String defaultMessage() {
    return message;
  }
}
