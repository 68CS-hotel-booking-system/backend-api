package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.RegisterResult;
import java.util.UUID;

public record RegisterResponse(
    UUID id,
    String email,
    String fullName,
    String phone,
    String status,
    boolean emailVerified,
    long version) {

  public static RegisterResponse from(RegisterResult result) {
    return new RegisterResponse(
        result.id(),
        result.email(),
        result.fullName(),
        result.phone(),
        result.status(),
        result.emailVerified(),
        result.version());
  }
}
