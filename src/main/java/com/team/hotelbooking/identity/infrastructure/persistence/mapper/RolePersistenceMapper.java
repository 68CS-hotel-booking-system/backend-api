package com.team.hotelbooking.identity.infrastructure.persistence.mapper;

import com.team.hotelbooking.identity.domain.model.Role;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;

public final class RolePersistenceMapper {

  private RolePersistenceMapper() {}

  public static Role toDomain(RoleJpaEntity entity) {
    return new Role(entity.getId(), entity.getRoleKey(), entity.getRoleName(), entity.isActive());
  }
}
