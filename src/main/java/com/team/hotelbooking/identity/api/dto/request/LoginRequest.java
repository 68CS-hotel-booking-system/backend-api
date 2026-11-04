package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(max = 128) String password) {

  public LoginRequest {
    email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return "LoginRequest[credentials=REDACTED]";
  }
}
