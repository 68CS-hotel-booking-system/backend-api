package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.result.CurrentUserResult;
import com.team.hotelbooking.identity.application.usecase.GetCurrentUserUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.util.UUID;

public class GetCurrentUserService implements GetCurrentUserUseCase {

  private final UserRepository userRepository;

  public GetCurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public CurrentUserResult execute(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCOUNT_UNAVAILABLE));

    if (user.status().equals(User.Status.BANNED) || user.status().equals(User.Status.INACTIVE)) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_UNAVAILABLE);
    }

    return CurrentUserResult.from(user);
  }
}
