package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.LoginResult;
import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant expiresAt,
    UserResponse user) {

  public static LoginResponse from(LoginResult result) {
    return from(result, false);
  }

  public static LoginResponse from(LoginResult result, boolean includeRefreshToken) {
    return new LoginResponse(
        result.accessToken(),
        includeRefreshToken ? result.refreshToken() : null,
        result.tokenType(),
        result.expiresAt(),
        result.user() == null ? null : UserResponse.from(result.user()));
  }

  public record UserResponse(
      UUID id,
      String email,
      String fullName,
      String phone,
      String status,
      boolean emailVerified,
      long version) {

    public static UserResponse from(LoginResult.UserInfo user) {
      return new UserResponse(
          user.id(),
          user.email(),
          user.fullName(),
          user.phone(),
          user.status(),
          user.emailVerified(),
          user.version());
    }
  }

  @Override
  public String toString() {
    return "LoginResponse[tokens=REDACTED]";
  }
}
