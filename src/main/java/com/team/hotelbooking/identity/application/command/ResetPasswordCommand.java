package com.team.hotelbooking.identity.application.command;

public record ResetPasswordCommand(String token, String newPassword) {

  @Override
  public String toString() {
    return "ResetPasswordCommand[token=REDACTED, newPassword=REDACTED]";
  }
}
