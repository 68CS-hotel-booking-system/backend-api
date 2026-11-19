package com.team.hotelbooking.identity.application.command;

public record LoginCommand(String email, String password, boolean rememberMe) {

  public LoginCommand(String email, String password) {
    this(email, password, false);
  }

  @Override
  public String toString() {
    return "LoginCommand[credentials=REDACTED]";
  }
}
