package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống.")
        @Size(max = 128, message = "Mật khẩu hiện tại không được vượt quá 128 ký tự.")
        String currentPassword,
    @NotBlank(message = "Mật khẩu mới không được để trống.")
        @Size(min = 15, max = 128, message = "Mật khẩu mới phải có từ 15 đến 128 ký tự.")
        String newPassword) {

  @Override
  public String toString() {
    return "ChangePasswordRequest[credentials=REDACTED]";
  }
}
