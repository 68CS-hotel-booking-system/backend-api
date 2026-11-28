package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.UpdateProfileCommand;
import com.team.hotelbooking.identity.application.result.CurrentUserResult;
import com.team.hotelbooking.identity.application.usecase.UpdateProfileUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;

public class UpdateProfileService implements UpdateProfileUseCase {
  private final UserRepository users;
  private final Clock clock;

  public UpdateProfileService(UserRepository users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Override
  public CurrentUserResult execute(UpdateProfileCommand command) {
    if (command == null || command.userId() == null || command.expectedVersion() < 0) {
      throw IdentityException.invalid("Dữ liệu cập nhật hồ sơ không hợp lệ.");
    }
    User user =
        users
            .findByIdForUpdate(command.userId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCOUNT_UNAVAILABLE));
    user.ensureCanLogin();
    if (user.version() != command.expectedVersion()) {
      throw new IdentityException(IdentityErrorCode.STALE_USER_VERSION);
    }
    User updated = user.updateProfile(command.fullName(), command.phone(), clock.instant());
    if (user.fullName().equals(updated.fullName()) && user.phone().equals(updated.phone())) {
      return CurrentUserResult.from(user);
    }
    if (!user.phone().equals(updated.phone()) && users.existsByPhone(updated.phone())) {
      throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_USED);
    }
    return CurrentUserResult.from(users.updateProfile(updated));
  }
}
