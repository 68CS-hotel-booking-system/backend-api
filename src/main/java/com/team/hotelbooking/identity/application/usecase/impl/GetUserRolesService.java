package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.GetUserRolesQuery;
import com.team.hotelbooking.identity.application.result.UserRolesResult;
import com.team.hotelbooking.identity.application.usecase.GetUserRolesUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;

public class GetUserRolesService implements GetUserRolesUseCase {

  private final UserRepository users;

  public GetUserRolesService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserRolesResult execute(GetUserRolesQuery query) {
    if (query == null || query.actorId() == null || query.targetUserId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện và tài khoản đích là bắt buộc.");
    }

    User actor =
        users
            .findById(query.actorId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));

    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }

    actor.ensureCanLogin();

    User target =
        users
            .findById(query.targetUserId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

    return new UserRolesResult(target.id(), target.roles().stream().sorted().toList());
  }
}
