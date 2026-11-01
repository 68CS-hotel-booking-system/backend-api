package com.team.hotelbooking.identity.infrastructure.persistence.mapper;

import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Locale;
import java.util.Set;

public final class UserPersistenceMapper {

  private UserPersistenceMapper() {}

  public static UserJpaEntity toNewEntity(User user) {
    UserJpaEntity entity = new UserJpaEntity();

    entity.setId(user.id());
    entity.setEmail(user.email());
    entity.setPasswordHash(user.passwordHash());
    entity.setFullName(user.fullName());
    entity.setPhone(user.phone());

    entity.setStatus(UserJpaEntity.DbStatus.valueOf(user.status().name().toLowerCase(Locale.ROOT)));

    entity.setEmailVerifiedAt(user.emailVerifiedAt());
    entity.setDeleted(false);
    entity.setCreatedAt(user.createdAt());
    entity.setUpdatedAt(user.updatedAt());

    // version để null: đây là entity mới.
    return entity;
  }

  public static User toDomain(UserJpaEntity entity, Set<String> roles) {

    return new User(
        entity.getId(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getFullName(),
        entity.getPhone(),
        User.Status.valueOf(entity.getStatus().name().toUpperCase(Locale.ROOT)),
        entity.getEmailVerifiedAt(),
        roles,
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }
}
