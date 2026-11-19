package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.UpdateUserStatusCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;

public interface UpdateUserStatusUseCase {

  AdminUserResult execute(UpdateUserStatusCommand command);
}
