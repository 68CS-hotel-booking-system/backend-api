package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {

  Optional<Role> findById(Integer roleId);

  Optional<Role> findByIdForUpdate(Integer roleId);

  Optional<Role> findByKeyForUpdate(String roleKey);

  List<Role> findAllActive();
}
