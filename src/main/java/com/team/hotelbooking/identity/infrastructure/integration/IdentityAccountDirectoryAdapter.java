package com.team.hotelbooking.identity.infrastructure.integration;

import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;

public class IdentityAccountDirectoryAdapter implements AccountDirectory {

  private final UserRepository users;

  public IdentityAccountDirectoryAdapter(UserRepository users) {
    this.users = users;
  }

  @Override
  public Optional<AccountInfo> findById(UUID userId) {
    return users
        .findById(userId)
        .map(
            user ->
                new AccountInfo(
                    user.id(),
                    user.fullName(),
                    user.email(),
                    user.status() == User.Status.ACTIVE,
                    user.emailVerifiedAt() != null,
                    user.roles()));
  }
}
