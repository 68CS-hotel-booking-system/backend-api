package com.team.hotelbooking.identity.domain.model;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import java.time.Instant;
import java.util.UUID;

public record PasswordResetToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    Instant createdAt,
    Instant usedAt,
    boolean revoked) {

  public PasswordResetToken consume(Instant now) {
    if (revoked) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    if (usedAt != null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_ALREADY_USED);
    }

    if (!expiresAt.isAfter(now)) {
      throw new IdentityException(IdentityErrorCode.TOKEN_EXPIRED);
    }

    return new PasswordResetToken(id, userId, tokenHash, expiresAt, createdAt, now, revoked);
  }
}
