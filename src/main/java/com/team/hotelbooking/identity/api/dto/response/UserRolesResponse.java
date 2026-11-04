package com.team.hotelbooking.identity.api.dto.response;

import com.team.hotelbooking.identity.application.result.UserRolesResult;
import java.util.List;
import java.util.UUID;

public record UserRolesResponse(UUID userId, List<String> roles) {

  public UserRolesResponse {
    roles = List.copyOf(roles);
  }

  public static UserRolesResponse from(UserRolesResult result) {
    return new UserRolesResponse(result.userId(), result.roles());
  }
}
