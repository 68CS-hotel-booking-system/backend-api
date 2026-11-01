package com.team.hotelbooking.identity.application.port;

public interface OpaqueTokens {

  IssuedToken generate();

  String hash(String rawToken);

  record IssuedToken(String raw, String hash) {
    @Override
    public String toString() {
      return "IssuedToken[REDACTED]";
    }
  }
}
