package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Họ tên không được để trống.")
        @Size(max = 100, message = "Họ tên không được quá 100 ký tự.")
        String fullName,
    @NotBlank(message = "Số điện thoại không được để trống.")
        @Size(max = 30, message = "Số điện thoại không được quá 30 ký tự.")
        String phone,
    @NotNull(message = "Phiên bản hồ sơ là bắt buộc.")
        @PositiveOrZero(message = "Phiên bản hồ sơ không hợp lệ.")
        Long expectedVersion) {
  public UpdateProfileRequest {
    fullName = fullName == null ? null : fullName.trim();
    phone = phone == null ? null : phone.trim();
  }
}
