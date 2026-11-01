package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.model.RefreshToken;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.RefreshTokenJpaRepository;
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

    tokens.saveAndFlush(entity);
  }
}
