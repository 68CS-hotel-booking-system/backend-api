package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.ListAdminUsersQuery;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.shared.pagination.PageResult;

public interface ListAdminUsersUseCase {

  PageResult<AdminUserResult> execute(ListAdminUsersQuery query);
}
