package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.RevokeRoleCommand;
import com.team.hotelbooking.identity.application.result.UserRolesResult;

public interface RevokeRoleUseCase {

  UserRolesResult execute(RevokeRoleCommand command);
}
