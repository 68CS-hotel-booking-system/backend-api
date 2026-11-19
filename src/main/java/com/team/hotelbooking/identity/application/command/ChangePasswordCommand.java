package com.team.hotelbooking.identity.application.command;

import java.util.UUID;

public record ChangePasswordCommand(UUID userId, String currentPassword, String newPassword) {

  @Override
  public String toString() {
    return "ChangePasswordCommand[credentials=REDACTED]";
  }
}
