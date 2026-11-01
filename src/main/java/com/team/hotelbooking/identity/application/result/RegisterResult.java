package com.team.hotelbooking.identity.application.result;

import com.team.hotelbooking.identity.domain.model.User;
import java.util.Locale;
import java.util.UUID;

public record RegisterResult(
    UUID id,
    String email,
    String fullName,
    String phone,
    String status,
    boolean emailVerified,
    long version) {

  public static RegisterResult from(User user) {
    return new RegisterResult(
        user.id(),
        user.email(),
        user.fullName(),
        user.phone(),
        user.status().name().toLowerCase(Locale.ROOT),
        user.emailVerifiedAt() != null,
        user.version());
  }
}
