package com.team.hotelbooking.identity.application.result;

import java.util.List;
import java.util.UUID;

public record UserRolesResult(UUID userId, List<String> roles) {

  public UserRolesResult {
    roles = List.copyOf(roles);
  }
}
