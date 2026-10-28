package com.team.hotelbooking.identity.application.result;

import com.team.hotelbooking.identity.domain.model.User;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record LoginResult(
    String accessToken, String refreshToken, String tokenType, Instant expiresAt, UserInfo user) {

  public static LoginResult from(
      User user, String accessToken, String refreshToken, Instant expiresAt) {

    return new LoginResult(
        accessToken,
        refreshToken,
        "Bearer",
        expiresAt,
        new UserInfo(
            user.id(),
            user.email(),
            user.fullName(),
            user.phone(),
            user.status().name().toLowerCase(Locale.ROOT),
            user.emailVerifiedAt() != null,
            user.version()));
  }

  public record UserInfo(
      UUID id,
      String email,
      String fullName,
      String phone,
      String status,
      boolean emailVerified,
      long version) {}

  @Override
  public String toString() {
    return "LoginResult[tokens=REDACTED]";
  }
}
