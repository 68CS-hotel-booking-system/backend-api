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
  ROOM_OPERATION_CONFLICT("Không thể thay đổi phục vụ phòng vì ảnh hưởng đến đặt phòng hiện có."),
  DUPLICATE_ROOM_NUMBER("Số phòng đã tồn tại trong khách sạn này."),
  INVALID_OCCUPANCY("Thông tin sức chứa hoặc số lượng khách không hợp lệ."),
  INVALID_PRICE("Giá cơ bản của loại phòng phải lớn hơn hoặc bằng 0."),
  INVALID_REQUEST("Yêu cầu chỉnh sửa không hợp lệ."),
  OWNER_NOT_FOUND("Không tìm thấy tài khoản chủ khách sạn."),
  OWNER_NOT_ELIGIBLE(
      "Tài khoản chủ khách sạn phải đang hoạt động, đã xác minh email và có quyền HOTEL_OWNER."),
  CITY_NOT_FOUND("Không tìm thấy thành phố."),
  HOTEL_SLUG_CONFLICT("Đường dẫn khách sạn đã tồn tại."),
  DUPLICATE_CUSTOM_AMENITY(
      "Tên tiện ích riêng đã tồn tại trong phạm vi này. Hãy sửa hoặc gắn lại tiện ích đã có."),
  AMENITY_CONCURRENT_UPDATE("Tiện ích đã được thay đổi. Vui lòng tải lại trước khi cập nhật."),
  AMENITY_NOT_FOUND("Không tìm thấy tiện ích."),
  AMENITY_CATEGORY_MISMATCH("Tiện ích không thuộc nhóm phù hợp."),
  HOTEL_NOT_READY("Khách sạn chưa đủ điều kiện mở bán."),
  INVALID_STATUS_TRANSITION("Không thể đổi trạng thái bán từ trạng thái hiện tại của khách sạn."),
  HOTEL_LOCKED("Khách sạn đang bị quản trị hệ thống khóa bán; chỉ quản trị hệ thống được gỡ khóa.");

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
