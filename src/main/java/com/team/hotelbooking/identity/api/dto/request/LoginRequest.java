package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Email @Size(max = 255) String email, @NotBlank String password) {

  @Override
  public String toString() {
    return "LoginRequest[credentials=REDACTED]";
  }
}
