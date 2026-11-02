package com.team.hotelbooking.identity.application.result;

import com.team.hotelbooking.identity.domain.model.User;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record CurrentUserResult(
    UUID id,
    String email,
    String fullName,
    String phone,
    String status,
    boolean emailVerified,
    List<String> roles,
    long version) {

  public static CurrentUserResult from(User user) {
    return new CurrentUserResult(
        user.id(),
        user.email(),
        user.fullName(),
        user.phone(),
        user.status().name().toLowerCase(Locale.ROOT),
        user.emailVerifiedAt() != null,
        user.roles().stream().sorted().toList(),
        user.version());
  }
}
