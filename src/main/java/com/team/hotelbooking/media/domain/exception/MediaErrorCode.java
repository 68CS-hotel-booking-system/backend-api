package com.team.hotelbooking.media.domain.exception;

import com.team.hotelbooking.shared.exception.ErrorCode;

public enum MediaErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu ảnh không hợp lệ."),
  UNAUTHORIZED("Bạn cần đăng nhập."),
  ACCESS_DENIED("Bạn không có quyền thao tác với ảnh này."),
  RESOURCE_NOT_FOUND("Không tìm thấy ảnh."),
  PAYLOAD_TOO_LARGE("Ảnh vượt quá dung lượng cho phép."),
  UNSUPPORTED_MEDIA_TYPE("Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP hợp lệ."),
  MEDIA_NOT_READY("Ảnh chưa sẵn sàng hoặc đã bị xóa."),
  CONFLICT("Dữ liệu ảnh đang có xung đột.");

  private final String message;

  MediaErrorCode(String message) {
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
