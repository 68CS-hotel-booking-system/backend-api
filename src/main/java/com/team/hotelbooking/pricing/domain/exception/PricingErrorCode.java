package com.team.hotelbooking.pricing.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum PricingErrorCode implements ErrorCode {
  INVALID_DATE_RANGE("Khoảng thời gian tra cứu giá không hợp lệ."),
  MAX_RANGE_EXCEEDED("Khoảng thời gian tra cứu giá tối đa là 90 ngày."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  ROOM_TYPE_NOT_FOUND("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."),
  INVALID_AMOUNT("Giá phòng phải lớn hơn 0."),
  MAX_STAY_EXCEEDED("Thời gian lưu trú tối đa là 30 đêm."),
  INVALID_QUOTE_REQUEST("Dữ liệu yêu cầu báo giá không hợp lệ."),
  INVALID_OCCUPANCY("Số lượng khách vượt quá sức chứa hoặc không hợp lệ."),
  RATE_NOT_AVAILABLE("Loại phòng chưa được thiết lập giá bán hợp lệ cho tất cả các đêm lưu trú."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  HOTEL_LOCKED("Khách sạn đang bị khóa bán; chưa thể thao tác giá phòng.");

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
