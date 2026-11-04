package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(@NotBlank @Size(max = 128) String refreshToken) {

  @Override
  public String toString() {
    return "RefreshTokenRequest[REDACTED]";
  }
}
