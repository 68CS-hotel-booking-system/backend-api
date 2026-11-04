package com.team.hotelbooking.hotel.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HotelErrorCode implements ErrorCode {
  HOTEL_NOT_FOUND("HOTEL_NOT_FOUND", "Không tìm thấy khách sạn."),
  ROOM_TYPE_NOT_FOUND(
      "ROOM_TYPE_NOT_FOUND", "Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."),
  ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Không tìm thấy phòng vật lý hoặc phòng không thuộc khách sạn."),
  DUPLICATE_ROOM_NUMBER("DUPLICATE_ROOM_NUMBER", "Số phòng đã tồn tại trong khách sạn này."),
  INVALID_OCCUPANCY("INVALID_OCCUPANCY", "Thông tin sức chứa hoặc số lượng khách không hợp lệ."),
  INVALID_PRICE("INVALID_PRICE", "Giá cơ bản của loại phòng phải lớn hơn hoặc bằng 0."),
  INVALID_REQUEST("INVALID_REQUEST", "Yêu cầu chỉnh sửa không hợp lệ.");

  private final String code;
  private final String defaultMessage;

  @Override
  public String code() {
    return code;
  }

  @Override
  public String defaultMessage() {
    return defaultMessage;
  }
}
