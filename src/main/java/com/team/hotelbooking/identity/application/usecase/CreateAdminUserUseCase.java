package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.CreateAdminUserCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;

public interface CreateAdminUserUseCase {

  AdminUserResult execute(CreateAdminUserCommand command);
}
