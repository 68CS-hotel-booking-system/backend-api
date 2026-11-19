package com.team.hotelbooking.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID userId,
    UUID familyId,
    String tokenHash,
    Instant expiresAt,
    boolean revoked,
    boolean rememberMe) {

  public RefreshToken(
      UUID id, UUID userId, UUID familyId, String tokenHash, Instant expiresAt, boolean revoked) {
    this(id, userId, familyId, tokenHash, expiresAt, revoked, false);
  }

  @Override
  public String toString() {
    return "RefreshToken[id=" + id + "]";
  }
}
