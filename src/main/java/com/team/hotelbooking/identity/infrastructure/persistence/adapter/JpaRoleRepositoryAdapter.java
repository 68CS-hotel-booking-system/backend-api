package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.model.Role;
import com.team.hotelbooking.identity.domain.repository.RoleRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.RoleJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaRoleRepositoryAdapter implements RoleRepository {

  private final RoleJpaRepository roles;

  @Override
  public Optional<Role> findById(Integer roleId) {
    return roles.findById(roleId).map(RolePersistenceMapper::toDomain);
  }

  @Override
  public Optional<Role> findByIdForUpdate(Integer roleId) {
    return roles.findByIdForUpdate(roleId).map(RolePersistenceMapper::toDomain);
  }

  @Override
  public Optional<Role> findByKeyForUpdate(String roleKey) {
    return roles.findByRoleKeyForUpdate(roleKey).map(RolePersistenceMapper::toDomain);
  }

  @Override
  public List<Role> findAllActive() {
    return roles.findAllByActiveTrueOrderByRoleKeyAsc().stream()
        .map(RolePersistenceMapper::toDomain)
        .toList();
  }
}
