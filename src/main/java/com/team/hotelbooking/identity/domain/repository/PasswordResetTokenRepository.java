package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
  void add(PasswordResetToken token);

  Optional<Instant> findLatestCreatedAtByUserId(UUID userId);

  Optional<UUID> findUserIdByTokenHash(String tokenHash);

  Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash);

  void markUsed(PasswordResetToken consumedToken);

  void revokeOtherUnusedByUserId(UUID userId, UUID excludedTokenId);

  void revokeAllUnusedByUserId(UUID userId);
}
