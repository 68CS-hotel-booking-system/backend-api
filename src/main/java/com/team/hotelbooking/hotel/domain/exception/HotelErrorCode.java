package com.team.hotelbooking.hotel.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum HotelErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu khách sạn không hợp lệ."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  ROOM_TYPE_NOT_FOUND("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."),
  ROOM_TYPE_INACTIVE("Loại phòng đã ngừng hoạt động."),
  ROOM_NOT_FOUND("Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."),
  ROOM_NUMBER_CONFLICT("Số phòng đã tồn tại trong khách sạn."),
  DUPLICATE_ROOM_NUMBER("Số phòng đã tồn tại trong khách sạn này."),
  INVALID_OCCUPANCY("Thông tin sức chứa hoặc số lượng khách không hợp lệ."),
  INVALID_PRICE("Giá cơ bản của loại phòng phải lớn hơn hoặc bằng 0."),
  INVALID_REQUEST("Yêu cầu chỉnh sửa không hợp lệ."),
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
