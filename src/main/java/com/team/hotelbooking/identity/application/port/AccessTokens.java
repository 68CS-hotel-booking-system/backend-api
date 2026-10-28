package com.team.hotelbooking.identity.application.port;

import com.team.hotelbooking.identity.domain.model.User;
import java.time.Instant;

public interface AccessTokens {

  IssuedToken issue(User user, Instant now);

  record IssuedToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
      return "IssuedToken[REDACTED]";
    }
  }
}
