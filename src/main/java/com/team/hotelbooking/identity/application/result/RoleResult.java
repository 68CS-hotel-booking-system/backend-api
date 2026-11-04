package com.team.hotelbooking.identity.application.result;

import com.team.hotelbooking.identity.domain.model.Role;

public record RoleResult(Integer id, String roleKey, String roleName) {

  public static RoleResult from(Role role) {
    return new RoleResult(role.id(), role.roleKey(), role.roleName());
  }
}
