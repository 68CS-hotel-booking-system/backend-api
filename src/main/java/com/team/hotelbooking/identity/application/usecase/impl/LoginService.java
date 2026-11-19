package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.LoginCommand;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.usecase.LoginUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;

public class LoginService implements LoginUseCase {

  private final UserRepository users;
  private final PasswordHasher passwords;
  private final LoginSessionIssuer sessionIssuer;
  private final String dummyPasswordHash;

  public LoginService(
      UserRepository users,
      PasswordHasher passwords,
      LoginSessionIssuer sessionIssuer,
      String dummyPasswordHash) {
    this.users = users;
    this.passwords = passwords;
    this.sessionIssuer = sessionIssuer;
    this.dummyPasswordHash = dummyPasswordHash;
  }

  @Override
  public LoginResult execute(LoginCommand command) {
    if (command.password() == null || command.password().isBlank()) {
      throw IdentityException.invalid("Mật khẩu không được để trống.");
    }

    String email = User.normalizeEmail(command.email());

    User found = users.findByEmail(email).orElse(null);

    String encoded = found == null ? dummyPasswordHash : found.passwordHash();

    boolean matches = passwords.matches(command.password(), encoded);

    if (found == null || !matches) {
      throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    User current =
        users
            .findByIdForUpdate(found.id())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS));

    if (!current.passwordHash().equals(found.passwordHash())) {
      throw new IdentityException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    return sessionIssuer.issue(current, command.rememberMe());
  }
}
