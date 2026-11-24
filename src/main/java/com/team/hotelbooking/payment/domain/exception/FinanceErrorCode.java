package com.team.hotelbooking.payment.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum FinanceErrorCode implements ErrorCode {
  VALIDATION_FAILED("Tham số tìm kiếm hoặc phân trang không hợp lệ."),
  PAYMENT_NOT_FOUND("Không tìm thấy giao dịch hoặc đơn đặt phòng của bạn."),
  PAYMENT_UNAVAILABLE("Thanh toán VNPAY chưa được cấu hình hoặc chưa được bật."),
  PAYMENT_QR_UNAVAILABLE("Chưa thể tạo mã QR VNPAY. Vui lòng thử lại sau."),
  BOOKING_NOT_PAYABLE("Đơn đã hết hạn, đã thanh toán hoặc không ở trạng thái chờ thanh toán."),
  INVALID_PAYMENT_AMOUNT(
      "VNPAY chỉ hỗ trợ VND, số tiền tối thiểu 5.000 đồng và trong giới hạn cho phép."),
  CONTRACT_CODE_ALREADY_EXISTS("Mã hợp đồng đã tồn tại trên hệ thống."),
  INVALID_CONTRACT_PERIOD("Thời gian kết thúc hợp đồng phải sau thời gian bắt đầu."),
  INVALID_COMMISSION_RATE("Tỷ lệ hoa hồng phải nằm trong khoảng từ 0% đến 100%."),
  ACTIVE_CONTRACT_PERIOD_OVERLAP(
      "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách sạn."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  USER_NOT_FOUND("Không tìm thấy người dùng được chỉ định."),
  ACCESS_DENIED("Bạn không có quyền xem thông tin tài chính của khách sạn này."),
  CONTRACT_NOT_FOUND("Không tìm thấy hợp đồng của khách sạn."),
  INVALID_CONTRACT_STATUS_TRANSITION("Trạng thái hợp đồng không cho phép thao tác này.");

  private final String defaultMessage;

  FinanceErrorCode(String defaultMessage) {
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
