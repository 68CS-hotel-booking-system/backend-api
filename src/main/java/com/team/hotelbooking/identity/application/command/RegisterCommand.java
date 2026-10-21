package com.team.hotelbooking.identity.application.command;

public record RegisterCommand(String email, String password, String fullName, String phone) {

  @Override
  public String toString() {
    return "RegisterCommand[credentials=REDACTED]";
  }
}
