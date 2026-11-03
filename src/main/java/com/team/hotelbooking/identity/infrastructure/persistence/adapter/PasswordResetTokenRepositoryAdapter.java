package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.PasswordResetToken;
import com.team.hotelbooking.identity.domain.repository.PasswordResetTokenRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

  private final PasswordResetTokenJpaRepository tokens;

  @Override
  public void add(PasswordResetToken token) {
    PasswordResetTokenJpaEntity entity = PasswordResetTokenMapper.toEntity(token);
    tokens.saveAndFlush(entity);
  }

  @Override
  public Optional<Instant> findLatestCreatedAtByUserId(UUID userId) {
    return tokens.findLatestCreatedAtByUserId(userId);
  }

  @Override
  public Optional<UUID> findUserIdByTokenHash(String tokenHash) {
    return tokens.findUserIdByTokenHash(tokenHash);
  }

  @Override
  public Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash) {

    return tokens
        .findByTokenHash(tokenHash)
        .map(entity -> PasswordResetTokenMapper.toDomain(entity));
  }

  @Override
  public void markUsed(PasswordResetToken consumedToken) {
    int updatedRows = tokens.markUsed(consumedToken.id(), consumedToken.usedAt());
    if (updatedRows != 1) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }
  }

  @Override
  public void revokeOtherUnusedByUserId(UUID userId, UUID excludedTokenId) {
    tokens.revokeOtherUnusedByUserId(userId, excludedTokenId);
  }

  @Override
  public void revokeAllUnusedByUserId(UUID userId) {
    tokens.revokeAllUnusedByUserId(userId);
  }
}
