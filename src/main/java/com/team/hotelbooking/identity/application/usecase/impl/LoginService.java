package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.LoginCommand;
import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.usecase.LoginUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class LoginService implements LoginUseCase {

  private final UserRepository users;
  private final PasswordHasher passwords;
  private final AccessTokens accessTokens;
  private final OpaqueTokens opaqueTokens;
  private final RefreshTokenRepository refreshTokens;
  private final Clock clock;
  private final Duration refreshTtl;
  private final String dummyPasswordHash;

  public LoginService(
      UserRepository users,
      PasswordHasher passwords,
      AccessTokens accessTokens,
      OpaqueTokens opaqueTokens,
      RefreshTokenRepository refreshTokens,
      Clock clock,
      Duration refreshTtl,
      String dummyPasswordHash) {

    if (refreshTtl.isNegative() || refreshTtl.isZero()) {
      throw new IllegalArgumentException("Refresh TTL must be positive.");
    }

    this.users = users;
    this.passwords = passwords;
    this.accessTokens = accessTokens;
    this.opaqueTokens = opaqueTokens;
    this.refreshTokens = refreshTokens;
    this.clock = clock;
    this.refreshTtl = refreshTtl;
    this.dummyPasswordHash = dummyPasswordHash;
  }

  @Override
  public LoginResult execute(LoginCommand command) {
    if (command.password() == null || command.password().isBlank()) {
      throw IdentityException.invalid("Mật khẩu không được để trống.");
    }

    String email = User.normalizeEmail(command.email());

    User found = users.findByEmail(email).orElse(null);

    String encoded = found == null ? dummyPasswordHash : found.passwordHash();

    boolean matches = passwords.matches(command.password(), encoded);

    if (found == null || !matches) {
      throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    User current =
        users
            .findByIdForUpdate(found.id())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS));

    if (!current.passwordHash().equals(found.passwordHash())) {
      throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    current.ensureCanLogin();

    Instant now = clock.instant();

    var access = accessTokens.issue(current, now);
    var refresh = opaqueTokens.generate();

    refreshTokens.add(
        new RefreshToken(
            UUID.randomUUID(),
            current.id(),
            UUID.randomUUID(),
            refresh.hash(),
            now.plus(refreshTtl),
            false));

    return LoginResult.from(current, access.value(), refresh.raw(), access.expiresAt());
  }
}
