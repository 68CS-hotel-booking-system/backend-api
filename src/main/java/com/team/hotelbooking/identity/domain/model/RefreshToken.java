package com.team.hotelbooking.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id, UUID userId, UUID familyId, String tokenHash, Instant expiresAt, boolean revoked) {

  @Override
  public String toString() {
    return "RefreshToken[id=" + id + "]";
  }
}
