package com.team.hotelbooking.identity.domain.repository;

import java.time.Instant;
import java.util.UUID;

public interface UserRoleRepository {

  void add(UUID userId, Integer roleId, Instant assignedAt);

  void remove(UUID userId, Integer roleId);

  long countActiveUsersByRoleId(Integer roleId);
}
