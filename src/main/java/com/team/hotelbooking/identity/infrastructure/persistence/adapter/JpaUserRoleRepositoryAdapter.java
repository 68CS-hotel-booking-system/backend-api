package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.repository.UserRoleRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserRoleRepositoryAdapter implements UserRoleRepository {

  private final UserRoleJpaRepository userRoles;

  @Override
  public void add(UUID userId, Integer roleId, Instant assignedAt) {
    UserRoleJpaEntity assignment = new UserRoleJpaEntity();
    assignment.setUserId(userId);
    assignment.setRoleId(roleId);
    assignment.setAssignedAt(assignedAt);

    userRoles.saveAndFlush(assignment);
  }

  @Override
  public void remove(UUID userId, Integer roleId) {
    userRoles.deleteAssignment(userId, roleId);
  }

  @Override
  public long countActiveUsersByRoleId(Integer roleId) {
    return userRoles.countActiveUsersByRoleId(roleId, UserJpaEntity.DbStatus.active);
  }
}
