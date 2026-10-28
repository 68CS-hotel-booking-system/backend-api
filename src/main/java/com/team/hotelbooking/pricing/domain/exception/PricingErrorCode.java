package com.team.hotelbooking.pricing.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum PricingErrorCode implements ErrorCode {
  INVALID_DATE_RANGE("Khoảng thời gian tra cứu giá không hợp lệ."),
  MAX_RANGE_EXCEEDED("Khoảng thời gian tra cứu giá tối đa là 90 ngày."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  ROOM_TYPE_NOT_FOUND("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn.");

  private final String defaultMessage;

  PricingErrorCode(String defaultMessage) {
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
