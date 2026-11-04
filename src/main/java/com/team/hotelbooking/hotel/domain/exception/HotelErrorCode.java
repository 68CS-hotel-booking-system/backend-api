package com.team.hotelbooking.hotel.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum HotelErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu khách sạn không hợp lệ."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  OWNER_NOT_FOUND("Không tìm thấy tài khoản chủ khách sạn."),
  OWNER_NOT_ELIGIBLE(
      "Tài khoản chủ khách sạn phải đang hoạt động, đã xác minh email và có quyền HOTEL_OWNER."),
  CITY_NOT_FOUND("Không tìm thấy thành phố."),
  HOTEL_SLUG_CONFLICT("Đường dẫn khách sạn đã tồn tại.");

  private final String message;

  HotelErrorCode(String message) {
    this.message = message;
  }

  @Override
  public String code() {
    return name();
  }

  @Override
  public String defaultMessage() {
    return message;
  }
}
