package com.team.hotelbooking.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {

  @Override
  public String toString() {
    return "VerifyEmailRequest[token=REDACTED]";
  }
}
