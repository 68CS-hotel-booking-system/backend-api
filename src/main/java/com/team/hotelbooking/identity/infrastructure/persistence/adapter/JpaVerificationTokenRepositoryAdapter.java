package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.model.VerificationToken;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.VerificationTokenJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaVerificationTokenRepositoryAdapter implements VerificationTokenRepository {

  private final VerificationTokenJpaRepository tokens;

  @Override
  public void add(VerificationToken token) {
    VerificationTokenJpaEntity entity = new VerificationTokenJpaEntity();

    entity.setId(token.id());
    entity.setUserId(token.userId());
    entity.setTokenHash(token.tokenHash());
    entity.setExpiresAt(token.expiresAt());
    entity.setCreatedAt(token.createdAt());

    tokens.saveAndFlush(entity);
  }

  @Override
  public Optional<VerificationToken> findByHashForUpdate(String tokenHash) {
    return tokens
        .findByTokenHash(tokenHash)
        .map(
            entity ->
                new VerificationToken(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getTokenHash(),
                    entity.getExpiresAt(),
                    entity.getCreatedAt(),
                    entity.getUsedAt(),
                    entity.getRevokedAt()));
  }

  @Override
  public void markUsed(VerificationToken token) {
    VerificationTokenJpaEntity entity = tokens.findById(token.id()).orElseThrow();

    entity.setUsedAt(token.usedAt());

    tokens.saveAndFlush(entity);
  }
}
