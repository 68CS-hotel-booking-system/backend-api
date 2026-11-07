package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 128) String password,
    Boolean rememberMe) {

  public LoginRequest(String email, String password) {
    this(email, password, false);
  }

  public LoginRequest {
    email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    rememberMe = rememberMe != null && rememberMe;
  }

  @Override
  public String toString() {
    return "LoginRequest[credentials=REDACTED]";
  }
}
