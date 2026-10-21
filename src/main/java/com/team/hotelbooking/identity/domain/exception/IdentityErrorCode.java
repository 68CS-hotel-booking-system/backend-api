package com.team.hotelbooking.identity.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum IdentityErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu đầu vào không hợp lệ."),
  EMAIL_ALREADY_USED("Email đã được đăng ký."),
  PHONE_ALREADY_USED("Số điện thoại đã được đăng ký."),
  RATE_LIMIT_EXCEEDED("Bạn thao tác quá nhanh. Vui lòng thử lại sau."),
  TOKEN_INVALID("Token không hợp lệ."),
  TOKEN_EXPIRED("Token đã hết hạn."),
  TOKEN_ALREADY_USED("Token đã được sử dụng."),
  CONFLICT("Trạng thái tài khoản không cho phép xác minh.");

  private final String message;

  IdentityErrorCode(String message) {
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
