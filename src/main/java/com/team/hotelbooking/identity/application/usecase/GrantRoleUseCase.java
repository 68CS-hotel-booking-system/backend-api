package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.GrantRoleCommand;
import com.team.hotelbooking.identity.application.result.UserRolesResult;

public interface GrantRoleUseCase {

  UserRolesResult execute(GrantRoleCommand command);
}
