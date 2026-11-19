package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.result.RoleResult;
import com.team.hotelbooking.identity.application.usecase.ListRolesUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.RoleRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;

public class ListRolesService implements ListRolesUseCase {

  private final UserRepository users;
  private final RoleRepository roles;

  public ListRolesService(UserRepository users, RoleRepository roles) {
    this.users = users;
    this.roles = roles;
  }

  @Override
  public List<RoleResult> execute(UUID actorId) {
    if (actorId == null) {
      throw IdentityException.invalid("Thông tin người thực hiện là bắt buộc.");
    }

    User actor =
        users
            .findById(actorId)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));

    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }

    actor.ensureCanLogin();

    return roles.findAllActive().stream().map(RoleResult::from).toList();
  }
}
