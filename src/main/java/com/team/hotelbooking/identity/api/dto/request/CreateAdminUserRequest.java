package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;

public record CreateAdminUserRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 20) String phone,
    @NotEmpty List<@NotBlank @Size(max = 50) String> roles) {

  public CreateAdminUserRequest {
    fullName = fullName == null ? null : fullName.trim();
    email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    phone = phone == null ? null : phone.trim();
  }
}
