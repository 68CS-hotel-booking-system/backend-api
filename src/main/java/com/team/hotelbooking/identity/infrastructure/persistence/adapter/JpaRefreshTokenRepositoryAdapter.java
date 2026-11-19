package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository tokens;

  @Override
  public void add(RefreshToken token) {
    RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();

    entity.setId(token.id());
    entity.setUserId(token.userId());
    entity.setFamilyId(token.familyId());
    entity.setTokenHash(token.tokenHash());
    entity.setExpiresAt(token.expiresAt());
    entity.setRevoked(token.revoked());
    entity.setRememberMe(token.rememberMe());

    tokens.saveAndFlush(entity);
  }

  @Override
  public Optional<UUID> findUserIdByTokenHash(String tokenHash) {
    return tokens.findUserIdByTokenHash(tokenHash);
  }

  @Override
  public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
    return tokens
        .findByTokenHashForUpdate(tokenHash)
        .map(
            entity ->
                new RefreshToken(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getFamilyId(),
                    entity.getTokenHash(),
                    entity.getExpiresAt(),
                    entity.isRevoked(),
                    entity.isRememberMe()));
  }

  @Override
  public void revokeFamily(UUID familyId) {
    tokens.revokeByFamilyId(familyId);
  }

  @Override
  public void revokeById(UUID tokenId) {
    tokens.revokeByTokenId(tokenId);
  }

  @Override
  public void revokeAllByUserId(UUID userId) {
    tokens.revokeAllByUserId(userId);
  }
}
