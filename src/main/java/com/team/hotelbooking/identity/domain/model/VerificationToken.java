package com.team.hotelbooking.identity.domain.model;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import java.time.Instant;
import java.util.UUID;

public record VerificationToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    Instant createdAt,
    Instant usedAt,
    Instant revokedAt) {

  public VerificationToken {
    if (!expiresAt.isAfter(createdAt)) {
      throw new IllegalArgumentException("Token expiry must be after creation time.");
    }
  }

  public VerificationToken(
      UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {

    this(id, userId, tokenHash, expiresAt, createdAt, null, null);
  }

  public VerificationToken consume(Instant now) {
    if (revokedAt != null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    if (usedAt != null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_ALREADY_USED);
    }

    if (!expiresAt.isAfter(now)) {
      throw new IdentityException(IdentityErrorCode.TOKEN_EXPIRED);
    }

    return new VerificationToken(id, userId, tokenHash, expiresAt, createdAt, now, revokedAt);
  }

  @Override
  public String toString() {
    return "VerificationToken[id=" + id + ", userId=" + userId + "]";
  }
}
