package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.result.VerifyEmailResult;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.VerificationToken;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class VerifyEmailService implements VerifyEmailUseCase {

  private final UserRepository users;
  private final VerificationTokenRepository tokens;
  private final OpaqueTokens opaqueTokens;
  private final Clock clock;

  private final AccessTokens accessTokens;
  private final RefreshTokenRepository refreshTokens;
  private final Duration refreshTtl;

  public VerifyEmailService(
      UserRepository users,
      VerificationTokenRepository tokens,
      OpaqueTokens opaqueTokens,
      Clock clock,
      AccessTokens accessTokens,
      RefreshTokenRepository refreshTokens,
      Duration refreshTtl) {
    this.users = users;
    this.tokens = tokens;
    this.opaqueTokens = opaqueTokens;
    this.clock = clock;
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.refreshTtl = refreshTtl;
  }

  @Override
  public VerifyEmailResult execute(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw IdentityException.invalid("Token không được để trống.");
    }

    if (!rawToken.matches("[A-Za-z0-9_-]{43}")) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    String tokenHash = opaqueTokens.hash(rawToken);
    UUID userId =
        tokens
            .findUserIdByTokenHash(tokenHash)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    User user =
        users
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    // Khóa user trước token để đồng bộ với gửi lại email và thu hồi token cũ.
    VerificationToken token =
        tokens
            .findByHashForUpdate(tokenHash)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));
    if (!token.userId().equals(user.id())) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    Instant now = clock.instant();

    VerificationToken consumed = token.consume(now);
    User verified = user.verifyEmail(now);

    users.updateVerification(verified);
    tokens.markUsed(consumed);

    Instant refreshExpiresAt = now.plus(refreshTtl);
    var access = accessTokens.issue(verified, now);
    var refresh = opaqueTokens.generate();
    boolean rememberMe = true;

    refreshTokens.add(
        new RefreshToken(
            UUID.randomUUID(),
            verified.id(),
            UUID.randomUUID(),
            refresh.hash(),
            refreshExpiresAt,
            false,
            rememberMe));
    LoginResult session =
        LoginResult.from(
            verified,
            access.value(),
            refresh.raw(),
            access.expiresAt(),
            rememberMe,
            refreshExpiresAt);
    return new VerifyEmailResult(true, session);
  }
}
