package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.GetUserRolesQuery;
import com.team.hotelbooking.identity.application.result.UserRolesResult;

public interface GetUserRolesUseCase {

  UserRolesResult execute(GetUserRolesQuery query);
}
