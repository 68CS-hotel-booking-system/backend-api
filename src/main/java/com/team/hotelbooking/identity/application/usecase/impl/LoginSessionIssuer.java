package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

public record LoginSessionIssuer(
    AccessTokens accessTokens,
    OpaqueTokens opaqueTokens,
    RefreshTokenRepository refreshTokens,
    Clock clock,
    Duration refreshTtl) {

  public LoginSessionIssuer {
    if (refreshTtl.isNegative() || refreshTtl.isZero()) {
      throw new IllegalArgumentException("Refresh TTL must be positive.");
    }
  }

  public LoginResult issue(User user, boolean rememberMe) {
    user.ensureCanLogin();
    var now = clock.instant();
    var refreshExpiresAt = now.plus(refreshTtl);
    var access = accessTokens.issue(user, now);
    var refresh = opaqueTokens.generate();
    refreshTokens.add(
        new RefreshToken(
            UUID.randomUUID(),
            user.id(),
            UUID.randomUUID(),
            refresh.hash(),
            refreshExpiresAt,
            false,
            rememberMe));

    return LoginResult.from(
        user, access.value(), refresh.raw(), access.expiresAt(), rememberMe, refreshExpiresAt);
  }
}
