package com.team.hotelbooking.identity.infrastructure.persistence.mapper;

import com.team.hotelbooking.identity.domain.model.PasswordResetToken;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;

public final class PasswordResetTokenMapper {

  private PasswordResetTokenMapper() {}

  public static PasswordResetTokenJpaEntity toEntity(PasswordResetToken token) {

    PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();

    entity.setId(token.id());
    entity.setUserId(token.userId());
    entity.setTokenHash(token.tokenHash());
    entity.setExpiresAt(token.expiresAt());
    entity.setCreatedAt(token.createdAt());
    entity.setUsedAt(token.usedAt());
    entity.setRevoked(token.revoked());

    return entity;
  }

  public static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {

    return new PasswordResetToken(
        entity.getId(),
        entity.getUserId(),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.getCreatedAt(),
        entity.getUsedAt(),
        entity.isRevoked());
  }
}
