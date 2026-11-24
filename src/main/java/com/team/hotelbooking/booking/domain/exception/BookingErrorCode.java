package com.team.hotelbooking.booking.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum BookingErrorCode implements ErrorCode {
  ADMIN_BOOKING_FORBIDDEN(
      "Tài khoản quản trị hệ thống không được đặt phòng cá nhân. Vui lòng sử dụng tài khoản khách hàng riêng."),
  BOOKING_NOT_CANCELLABLE(
      "Chỉ được hủy đơn chưa thanh toán. Vui lòng liên hệ hỗ trợ với đơn đã xác nhận."),
  BOOKING_PAYMENT_IN_PROGRESS(
      "Đơn có giao dịch đã thanh toán hoặc phiên thanh toán còn hiệu lực. Vui lòng kiểm tra thanh toán hoặc liên hệ hỗ trợ."),
  VALIDATION_FAILED("Dữ liệu đặt phòng không hợp lệ."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  EMAIL_NOT_VERIFIED("Tài khoản chưa xác minh email không được phép tạo đơn đặt phòng."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn hoặc khách sạn chưa mở bán."),
  CONTRACT_NOT_EFFECTIVE("Khách sạn tạm ngừng nhận đặt phòng do chưa có hợp đồng hiệu lực."),
  ROOM_TYPE_NOT_FOUND("Không tìm thấy loại phòng hoặc loại phòng không thuộc khách sạn."),
  BOOKING_NOT_FOUND("Không tìm thấy đơn đặt phòng."),
  INVALID_DATE_RANGE("Khoảng ngày nhận và trả phòng không hợp lệ."),
  MAX_STAY_EXCEEDED("Thời gian lưu trú tối đa là 30 đêm."),
  INVALID_OCCUPANCY("Số lượng khách vượt quá sức chứa hoặc không hợp lệ."),
  RATE_NOT_AVAILABLE("Loại phòng chưa có giá bán hợp lệ cho tất cả các đêm lưu trú."),
  PRICE_CHANGED(
      "Giá lưu trú đã thay đổi so với báo giá trước đó. Vui lòng kiểm tra và xác nhận lại báo giá mới."),
  IDEMPOTENCY_CONFLICT("Khóa chống lặp đã được sử dụng cho một yêu cầu đặt phòng khác.");

  private final String defaultMessage;

  BookingErrorCode(String defaultMessage) {
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
