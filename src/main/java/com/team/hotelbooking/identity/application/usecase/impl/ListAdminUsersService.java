package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.ListAdminUsersQuery;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.identity.application.usecase.ListAdminUsersUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.UserSearchCriteria;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.shared.pagination.PageResult;

public final class ListAdminUsersService implements ListAdminUsersUseCase {

  private final UserRepository users;

  public ListAdminUsersService(UserRepository users) {
    this.users = users;
  }

  @Override
  public PageResult<AdminUserResult> execute(ListAdminUsersQuery query) {

    if (query.actorId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện là bắt buộc.");
    }

    User actor =
        users
            .findById(query.actorId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));

    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }

    actor.ensureCanLogin();

    if (query.createdFrom() != null
        && query.createdTo() != null
        && !query.createdFrom().isBefore(query.createdTo())) {
      throw IdentityException.invalid("Thời gian bắt đầu phải trước thời gian kết thúc.");
    }

    UserSearchCriteria criteria =
        new UserSearchCriteria(
            query.search(),
            query.status(),
            query.role(),
            query.createdFrom(),
            query.createdTo(),
            query.sort());

    PageResult<User> page = users.findAll(criteria, query.pagination());

    return page.map(AdminUserResult::from);
  }
}
