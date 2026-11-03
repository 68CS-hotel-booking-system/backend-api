package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.CurrentUserResult;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String email,
    String fullName,
    String phone,
    String status,
    boolean emailVerified,
    List<String> roles,
    long version) {

  public static CurrentUserResponse from(CurrentUserResult result) {
    return new CurrentUserResponse(
        result.id(),
        result.email(),
        result.fullName(),
        result.phone(),
        result.status(),
        result.emailVerified(),
        List.copyOf(result.roles()),
        result.version());
  }
}
