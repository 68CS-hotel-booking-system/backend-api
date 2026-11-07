package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.UpdateUserStatusCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.identity.application.usecase.UpdateUserStatusUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.Role;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.RoleRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.UserRoleRepository;
import java.time.Clock;
import java.util.Locale;

public final class UpdateUserStatusService implements UpdateUserStatusUseCase {

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final RefreshTokenRepository refreshTokens;
  private final Clock clock;

  public UpdateUserStatusService(
      UserRepository users,
      RoleRepository roles,
      UserRoleRepository userRoles,
      RefreshTokenRepository refreshTokens,
      Clock clock) {
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
    this.refreshTokens = refreshTokens;
    this.clock = clock;
  }

  @Override
  public AdminUserResult execute(UpdateUserStatusCommand command) {
    if (command == null || command.actorId() == null || command.userId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện và tài khoản đích là bắt buộc.");
    }
    if (command.expectedVersion() < 0) {
      throw IdentityException.invalid("Phiên bản tài khoản không được âm.");
    }
    if (command.reason() != null && command.reason().length() > 500) {
      throw IdentityException.invalid("Lý do thay đổi trạng thái không được vượt quá 500 ký tự.");
    }
    User.Status newStatus = parseStatus(command.status());

    // Cùng thứ tự khóa với RevokeRoleService: vai trò ADMIN trước tài khoản.
    // Kiểm tra người thực hiện sau khi có khóa để đọc quyền/trạng thái mới nhất.
    Role adminRole =
        roles
            .findByKeyForUpdate("ADMIN")
            .filter(Role::active)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ROLE_UNAVAILABLE));

    User actor =
        users
            .findById(command.actorId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));
    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }
    actor.ensureCanLogin();

    User target =
        users
            .findByIdForUpdate(command.userId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    if (target.version() != command.expectedVersion()) {
      throw new IdentityException(IdentityErrorCode.STALE_USER_VERSION);
    }
    if (target.status() == newStatus) {
      return AdminUserResult.from(target);
    }
    if (actor.id().equals(target.id())) {
      throw new IdentityException(IdentityErrorCode.SELF_STATUS_CHANGE_FORBIDDEN);
    }
    if (target.roles().contains("ADMIN")
        && target.status() == User.Status.ACTIVE
        && target.emailVerifiedAt() != null
        && newStatus != User.Status.ACTIVE
        && userRoles.countActiveUsersByRoleId(adminRole.id()) <= 1) {
      throw new IdentityException(IdentityErrorCode.LAST_ACTIVE_ADMIN_STATUS_CHANGE_FORBIDDEN);
    }

    User updated = target.changeStatus(newStatus, clock.instant());
    String reason = command.reason() == null ? null : command.reason().trim();
    User saved = users.updateStatus(updated, actor.id(), reason);
    refreshTokens.revokeAllByUserId(target.id());

    return AdminUserResult.from(saved);
  }

  private static User.Status parseStatus(String status) {
    if (status == null || status.isBlank()) {
      throw IdentityException.invalid("Trạng thái đích là bắt buộc.");
    }
    try {
      return User.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw IdentityException.invalid(
          "Trạng thái đích phải là active, inactive, pending hoặc banned.");
    }
  }
}
