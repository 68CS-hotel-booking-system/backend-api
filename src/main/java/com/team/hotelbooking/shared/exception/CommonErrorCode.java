package com.team.hotelbooking.shared.exception;

/** Technical error codes shared across modules. Business codes belong to their own modules. */
public enum CommonErrorCode implements ErrorCode {
  VALIDATION_FAILED("Dữ liệu đầu vào không hợp lệ."),
  INTERNAL_ERROR("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."),
  CONCURRENT_UPDATE("Dữ liệu đã được cập nhật bởi thao tác khác. Vui lòng tải lại và thử lại."),
  BAD_REQUEST("Yêu cầu không hợp lệ. Kiểm tra tham số và kiểu dữ liệu."),
  MALFORMED_REQUEST("Nội dung yêu cầu bị thiếu hoặc không đúng định dạng."),
  UNAUTHORIZED("Bạn cần đăng nhập để thực hiện thao tác này."),
  ACCESS_DENIED("Bạn không có quyền thực hiện thao tác này."),
  RESOURCE_NOT_FOUND("Không tìm thấy tài nguyên được yêu cầu."),
  METHOD_NOT_ALLOWED("Phương thức HTTP không được hỗ trợ."),
  NOT_ACCEPTABLE("Định dạng phản hồi được yêu cầu không được hỗ trợ."),
  CONFLICT("Yêu cầu xung đột với trạng thái hiện tại của dữ liệu."),
  PAYLOAD_TOO_LARGE("Nội dung yêu cầu vượt quá dung lượng cho phép."),
  UNSUPPORTED_MEDIA_TYPE("Định dạng nội dung yêu cầu không được hỗ trợ."),
  REQUEST_FAILED("Không thể xử lý yêu cầu.");

  private final String defaultMessage;

  CommonErrorCode(String defaultMessage) {
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
