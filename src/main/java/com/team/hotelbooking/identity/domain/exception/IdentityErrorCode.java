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
  CONFLICT("Trạng thái tài khoản không cho phép xác minh."),
  INVALID_CREDENTIALS("Email hoặc mật khẩu không đúng."),
  ACCOUNT_LOCKED("Tài khoản đang bị khóa."),
  EMAIL_NOT_VERIFIED("Vui lòng xác minh email trước khi đăng nhập."),
  INVALID_REFRESH_TOKEN("Refresh token không hợp lệ hoặc đã hết hạn."),
  REFRESH_TOKEN_REUSED("Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."),
  ACCOUNT_UNAVAILABLE("Tài khoản không còn khả dụng."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  USER_NOT_FOUND("Không tìm thấy tài khoản."),
  ROLE_UNAVAILABLE("Vai trò không tồn tại hoặc đã bị vô hiệu hóa."),
  CUSTOMER_ROLE_REMOVAL_FORBIDDEN("Không thể thu hồi vai trò khách hàng mặc định của tài khoản."),
  SELF_ADMIN_ROLE_REMOVAL_FORBIDDEN("Bạn không thể tự thu hồi quyền quản trị của mình."),
  LAST_ACTIVE_ADMIN("Không thể thu hồi quyền của quản trị viên hoạt động cuối cùng."),
  CURRENT_PASSWORD_INCORRECT("Mật khẩu hiện tại không đúng."),
  NEW_PASSWORD_SAME_AS_CURRENT("Mật khẩu mới phải khác mật khẩu hiện tại."),
  ;

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
