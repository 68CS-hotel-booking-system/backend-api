package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.VerificationToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
  void add(VerificationToken token);

  Optional<VerificationToken> findByHashForUpdate(String tokenHash);

  Optional<UUID> findUserIdByTokenHash(String tokenHash);

  Optional<Instant> findLatestCreatedAtByUserId(UUID userId);

  void revokeAllUnusedByUserId(UUID userId, Instant revokedAt);

  void markUsed(VerificationToken token);
}
