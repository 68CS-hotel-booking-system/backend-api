package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.RevokeRoleCommand;
import com.team.hotelbooking.identity.application.result.UserRolesResult;
import com.team.hotelbooking.identity.application.usecase.RevokeRoleUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.Role;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RoleRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RevokeRoleService implements RevokeRoleUseCase {

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;

  public RevokeRoleService(
      UserRepository users, RoleRepository roles, UserRoleRepository userRoles) {
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
  }

  @Override
  public UserRolesResult execute(RevokeRoleCommand command) {
    if (command == null || command.actorId() == null || command.targetUserId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện và tài khoản đích là bắt buộc.");
    }

    if (command.roleId() == null || command.roleId() < 1) {
      throw IdentityException.invalid("ID vai trò phải lớn hơn 0.");
    }

    // Khóa vai trò trước tài khoản để tuần tự hóa việc thu hồi, đặc biệt với ADMIN.
    Role role =
        roles
            .findByIdForUpdate(command.roleId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ROLE_UNAVAILABLE));

    if (!role.active()) {
      throw new IdentityException(IdentityErrorCode.ROLE_UNAVAILABLE);
    }

    // Đọc quyền/trạng thái sau khi có khóa để không dùng dữ liệu trước lúc chờ khóa.
    requireAdmin(command.actorId());

    User target =
        users
            .findByIdForUpdate(command.targetUserId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

    if (role.roleKey().equals("CUSTOMER")) {
      throw new IdentityException(IdentityErrorCode.CUSTOMER_ROLE_REMOVAL_FORBIDDEN);
    }

    Set<String> updatedRoles = new HashSet<>(target.roles());
    if (!updatedRoles.contains(role.roleKey())) {
      return new UserRolesResult(target.id(), updatedRoles.stream().sorted().toList());
    }

    if (role.roleKey().equals("ADMIN")) {
      if (command.actorId().equals(target.id())) {
        throw new IdentityException(IdentityErrorCode.SELF_ADMIN_ROLE_REMOVAL_FORBIDDEN);
      }

      boolean targetCanLogin =
          target.status() == User.Status.ACTIVE && target.emailVerifiedAt() != null;
      if (targetCanLogin && userRoles.countActiveUsersByRoleId(role.id()) <= 1) {
        throw new IdentityException(IdentityErrorCode.LAST_ACTIVE_ADMIN);
      }
    }

    userRoles.remove(target.id(), role.id());
    updatedRoles.remove(role.roleKey());

    return new UserRolesResult(target.id(), updatedRoles.stream().sorted().toList());
  }

  private void requireAdmin(UUID actorId) {
    User actor =
        users
            .findById(actorId)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));

    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }

    actor.ensureCanLogin();
  }
}
