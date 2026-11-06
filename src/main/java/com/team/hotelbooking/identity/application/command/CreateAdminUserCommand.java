package com.team.hotelbooking.identity.application.command;

import java.util.List;
import java.util.UUID;

public record CreateAdminUserCommand(
    UUID actorId, String fullName, String email, String phone, List<String> roles) {

  public CreateAdminUserCommand {
    roles = List.copyOf(roles);
  }
}
