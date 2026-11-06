package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.CreateAdminUserCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.identity.application.usecase.CreateAdminUserUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;

/**
 * Khung xử lý; bổ sung dependency qua constructor và bean trong IdentityConfig khi viết nghiệp vụ.
 */
public final class CreateAdminUserService implements CreateAdminUserUseCase {

  @Override
  public AdminUserResult execute(CreateAdminUserCommand command) {

    throw new IdentityException(IdentityErrorCode.NOT_IMPLEMENTED);
  }
}
