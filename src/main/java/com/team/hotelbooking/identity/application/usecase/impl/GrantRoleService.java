package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.GrantRoleCommand;
import com.team.hotelbooking.identity.application.result.UserRolesResult;
import com.team.hotelbooking.identity.application.usecase.GrantRoleUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.Role;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RoleRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.UserRoleRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

public class GrantRoleService implements GrantRoleUseCase {

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final Clock clock;

  public GrantRoleService(
      UserRepository users, RoleRepository roles, UserRoleRepository userRoles, Clock clock) {
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
    this.clock = clock;
  }

  @Override
  public UserRolesResult execute(GrantRoleCommand command) {
    if (command == null || command.actorId() == null || command.targetUserId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện và tài khoản đích là bắt buộc.");
    }

    if (command.roleId() == null || command.roleId() < 1) {
      throw IdentityException.invalid("ID vai trò phải lớn hơn 0.");
    }

    User actor =
        users
            .findById(command.actorId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));

    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }

    actor.ensureCanLogin();

    // Khóa tài khoản trước khi kiểm tra vai trò để các yêu cầu cấp quyền không ghi trùng.
    User target =
        users
            .findByIdForUpdate(command.targetUserId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

    target.ensureCanLogin();

    Role role =
        roles
            .findById(command.roleId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ROLE_UNAVAILABLE));

    if (!role.active()) {
      throw new IdentityException(IdentityErrorCode.ROLE_UNAVAILABLE);
    }

    Set<String> updatedRoles = new HashSet<>(target.roles());
    if (updatedRoles.add(role.roleKey())) {
      userRoles.add(target.id(), role.id(), clock.instant());
    }

    return new UserRolesResult(target.id(), updatedRoles.stream().sorted().toList());
  }
}
