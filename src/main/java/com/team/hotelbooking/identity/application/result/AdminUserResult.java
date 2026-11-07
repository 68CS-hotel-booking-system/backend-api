package com.team.hotelbooking.identity.application.result;

import com.team.hotelbooking.identity.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record AdminUserResult(
    UUID id,
    String fullName,
    String email,
    String phone,
    List<String> roles,
    String status,
    boolean emailVerified,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public AdminUserResult {
    roles = List.copyOf(roles);
  }

  public static AdminUserResult from(User user) {
    return new AdminUserResult(
        user.id(),
        user.fullName(),
        user.email(),
        user.phone(),
        user.roles().stream().sorted().toList(),
        user.status().name().toLowerCase(Locale.ROOT),
        user.emailVerifiedAt() != null,
        user.createdAt(),
        user.updatedAt(),
        user.version());
  }
}
