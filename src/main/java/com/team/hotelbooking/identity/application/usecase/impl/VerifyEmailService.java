package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.result.VerifyEmailResult;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.VerificationToken;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Instant;

public class VerifyEmailService implements VerifyEmailUseCase {

  private final UserRepository users;
  private final VerificationTokenRepository tokens;
  private final OpaqueTokens opaqueTokens;
  private final Clock clock;

  public VerifyEmailService(
      UserRepository users,
      VerificationTokenRepository tokens,
      OpaqueTokens opaqueTokens,
      Clock clock) {

    this.users = users;
    this.tokens = tokens;
    this.opaqueTokens = opaqueTokens;
    this.clock = clock;
  }

  @Override
  public VerifyEmailResult execute(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw IdentityException.invalid("Token không được để trống.");
    }

    if (!rawToken.matches("[A-Za-z0-9_-]{43}")) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    VerificationToken token =
        tokens
            .findByHashForUpdate(opaqueTokens.hash(rawToken))
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    User user =
        users
            .findByIdForUpdate(token.userId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    Instant now = clock.instant();

    VerificationToken consumed = token.consume(now);
    User verified = user.verifyEmail(now);

    users.updateVerification(verified);
    tokens.markUsed(consumed);

    return new VerifyEmailResult(true);
  }
}
