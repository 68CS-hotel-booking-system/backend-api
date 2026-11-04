package com.team.hotelbooking.payment.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum FinanceErrorCode implements ErrorCode {
  CONTRACT_CODE_ALREADY_EXISTS("Mã hợp đồng đã tồn tại trên hệ thống."),
  INVALID_CONTRACT_PERIOD("Thời gian kết thúc hợp đồng phải sau thời gian bắt đầu."),
  INVALID_COMMISSION_RATE("Tỷ lệ hoa hồng phải nằm trong khoảng từ 0% đến 100%."),
  ACTIVE_CONTRACT_PERIOD_OVERLAP(
      "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách sạn."),
  HOTEL_NOT_FOUND("Không tìm thấy khách sạn."),
  USER_NOT_FOUND("Không tìm thấy người dùng được chỉ định."),
  FINANCIAL_ACCESS_NOT_FOUND(
      "Chưa có thiết lập phân quyền tài chính cho người dùng tại khách sạn này."),
  CONCURRENT_MODIFICATION_DETECTED(
      "Dữ liệu phân quyền đã được cập nhật bởi một thao tác khác. Vui lòng tải lại và thử lại."),
  ACCESS_DENIED("Bạn không có quyền xem thông tin tài chính của khách sạn này.");

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
