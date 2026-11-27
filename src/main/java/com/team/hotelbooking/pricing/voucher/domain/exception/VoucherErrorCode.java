package com.team.hotelbooking.pricing.voucher.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum VoucherErrorCode implements ErrorCode {
  VOUCHER_INVALID("Mã giảm giá không hợp lệ hoặc không áp dụng cho đơn này."),
  VOUCHER_UNAVAILABLE("Mã giảm giá đã tạm dừng, hết hạn hoặc hết lượt."),
  VOUCHER_DUPLICATE("Mã giảm giá đã tồn tại tại khách sạn."),
  VOUCHER_NOT_FOUND("Không tìm thấy mã giảm giá."),
  VOUCHER_ACCESS_DENIED("Chỉ owner hiện tại được quản lý voucher của khách sạn."),
  VOUCHER_VERSION_CONFLICT("Voucher đã được thay đổi. Vui lòng tải lại."),
  VOUCHER_PRICE_CHANGED("Mức giảm đã thay đổi. Vui lòng lấy lại báo giá.");
  private final String message;

  VoucherErrorCode(String message) {
    this.message = message;
  }

  public String code() {
    return name();
  }

  public String defaultMessage() {
    return message;
  }
}
