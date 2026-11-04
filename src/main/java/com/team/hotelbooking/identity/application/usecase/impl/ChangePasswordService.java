package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.ChangePasswordCommand;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.result.ChangePasswordResult;
import com.team.hotelbooking.identity.application.usecase.ChangePasswordUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.PasswordResetTokenRepository;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.service.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;

public class ChangePasswordService implements ChangePasswordUseCase {

  private final UserRepository users;
  private final PasswordHasher passwords;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final Clock clock;

  public ChangePasswordService(
      UserRepository users,
      PasswordHasher passwords,
      RefreshTokenRepository refreshTokens,
      PasswordResetTokenRepository resetTokens,
      Clock clock) {

    this.users = users;
    this.passwords = passwords;
    this.refreshTokens = refreshTokens;
    this.resetTokens = resetTokens;
    this.clock = clock;
  }

  @Override
  public ChangePasswordResult execute(ChangePasswordCommand command) {
    if (command == null || command.userId() == null) {
      throw new IdentityException(IdentityErrorCode.VALIDATION_FAILED);
    }

    if (command.currentPassword() == null
        || command.currentPassword().isBlank()
        || command.currentPassword().length() > 128) {
      throw new IdentityException(IdentityErrorCode.VALIDATION_FAILED);
    }

    PasswordPolicy.validateNewPassword(command.newPassword());

    User user =
        users
            .findByIdForUpdate(command.userId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCOUNT_UNAVAILABLE));

    if (user.status() == User.Status.BANNED || user.status() == User.Status.INACTIVE) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_LOCKED);
    }

    boolean currentPasswordCorrect =
        passwords.matches(command.currentPassword(), user.passwordHash());

    if (!currentPasswordCorrect) {
      throw new IdentityException(IdentityErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    String newPasswordHash = passwords.hash(command.newPassword());

    Instant now = clock.instant();

    User updatedUser = user.resetPassword(newPasswordHash, now);

    users.updatePassword(updatedUser);

    resetTokens.revokeAllUnusedByUserId(user.id());

    refreshTokens.revokeAllByUserId(user.id());

    return new ChangePasswordResult(true);
  }
}
