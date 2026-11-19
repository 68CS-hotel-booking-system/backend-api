package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

  void add(RefreshToken token);

  Optional<UUID> findUserIdByTokenHash(String tokenHash);

  Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

  void revokeFamily(UUID family);

  void revokeById(UUID tokenId);

  void revokeAllByUserId(UUID userId);
}
