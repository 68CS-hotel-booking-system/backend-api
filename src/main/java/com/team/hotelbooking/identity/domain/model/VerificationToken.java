package com.team.hotelbooking.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record VerificationToken(
    UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {

  public VerificationToken {
    if (!expiresAt.isAfter(createdAt)) {
      throw new IllegalArgumentException("Token expiry must be after creation time.");
    }
  }

  @Override
  public String toString() {
    return "VerificationToken[id=" + id + ", userId=" + userId + "]";
  }
}
