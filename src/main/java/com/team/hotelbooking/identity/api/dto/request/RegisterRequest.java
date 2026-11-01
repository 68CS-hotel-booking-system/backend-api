package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 15, max = 128) String password,
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Size(max = 20) String phone) {

  public RegisterRequest {
    email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);

    fullName = fullName == null ? null : fullName.trim();

    phone = phone == null ? null : phone.trim();
  }

  @Override
  public String toString() {
    return "RegisterRequest[credentials=REDACTED]";
  }
}
