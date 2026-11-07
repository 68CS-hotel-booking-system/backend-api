package com.team.hotelbooking.identity.application.result;

public record VerifyEmailResult(boolean accepted, LoginResult session) {
  public VerifyEmailResult(boolean accepted) {
    this(accepted, null);
  }
}
