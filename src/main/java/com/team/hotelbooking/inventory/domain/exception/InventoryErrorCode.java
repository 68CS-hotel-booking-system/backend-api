package com.team.hotelbooking.inventory.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum InventoryErrorCode implements ErrorCode {
  INVALID_DATE_RANGE("Khoảng thời gian lưu trú không hợp lệ."),
  MAX_STAY_EXCEEDED("Thời gian lưu trú tối đa không vượt quá 30 đêm."),
  INVALID_SEARCH_CRITERIA("Điều kiện tìm kiếm khách sạn không hợp lệ."),
  ACCESS_DENIED("Bạn không có quyền quản lý khách sạn này."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn hoặc khách sạn chưa mở bán."),
  ROOM_TYPE_INACTIVE("Loại phòng không tồn tại hoặc đã ngừng bán."),
  RATE_NOT_SET("Loại phòng chưa được thiết lập giá hợp lệ cho toàn bộ kỳ lưu trú."),
  INSUFFICIENT_INVENTORY("Số lượng phòng trống không đủ đáp ứng liên tục cho toàn bộ kỳ lưu trú."),
  HOLD_EXPIRED("Thời gian giữ chỗ đặt phòng đã hết hạn hoặc không còn hiệu lực."),
  ROOM_NOT_FOUND("Không tìm thấy phòng vật lý hoặc phòng đã ngừng phục vụ."),
  ROOM_BLOCK_NOT_FOUND("Không tìm thấy lịch chặn phòng."),
  ROOM_BLOCK_OVERLAP("Phòng vật lý đã có lịch chặn đang hoạt động chồng lấn khoảng ngày này."),
  ROOM_BLOCK_CONFLICTS_RESERVATION(
      "Không thể chặn phòng vì làm mất khả năng phân bổ phòng liên tục cho các đơn đang giữ hoặc đã xác nhận."),
  ROOM_BLOCK_ALREADY_RELEASED("Lịch chặn phòng đã được giải phóng trước đó, không thể chỉnh sửa."),
  BOOKING_NOT_FOUND("Không tìm thấy đơn đặt phòng hoặc đơn không thuộc khách sạn này."),
  INVALID_STAY_OPERATION("Thao tác lưu trú không hợp lệ ở trạng thái hiện tại của đơn đặt phòng."),
  ROOM_ASSIGNMENT_CONFLICT("Phân công phòng vật lý bị trùng lịch hoặc không hợp lệ."),
  ROOM_NOT_READY("Phòng vật lý chưa ở trạng thái sẵn sàng (READY) để khách nhận phòng.");

  private final String defaultMessage;

  InventoryErrorCode(String defaultMessage) {
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
