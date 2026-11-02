package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.ResetPasswordCommand;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.result.ResetPasswordResult;
import com.team.hotelbooking.identity.application.usecase.ResetPasswordUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.PasswordResetToken;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.PasswordResetTokenRepository;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.service.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class ResetPasswordService implements ResetPasswordUseCase {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OpaqueTokens opaqueTokens;
  private final PasswordHasher passwordHasher;
  private final Clock clock;

  public ResetPasswordService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokens opaqueTokens,
      PasswordHasher passwordHasher,
      Clock clock) {

    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.opaqueTokens = opaqueTokens;
    this.passwordHasher = passwordHasher;
    this.clock = clock;
  }

  @Override
  public ResetPasswordResult execute(ResetPasswordCommand command) {

    String rawToken = command.token();

    if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    if (rawToken.isBlank() || rawToken.isEmpty()) {
      throw new IdentityException(IdentityErrorCode.VALIDATION_FAILED);
    }

    PasswordPolicy.validateNewPassword(command.newPassword());

    var tokenHash = opaqueTokens.hash(rawToken);

    UUID userId = passwordResetTokenRepository.findUserIdByTokenHash(tokenHash).orElse(null);

    if (userId == null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    User user = userRepository.findByIdForUpdate(userId).orElse(null);

    if (user == null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    var token = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash).orElse(null);

    if (token == null) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    if (!token.userId().equals(user.id())) {
      throw new IdentityException(IdentityErrorCode.TOKEN_INVALID);
    }

    Instant now = clock.instant();

    // Kiểm tra revoked, usedAt và expiresAt
    PasswordResetToken consumedToken = token.consume(now);

    if (user.status() == User.Status.BANNED || user.status() == User.Status.INACTIVE) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_LOCKED);
    }

    String newPasswordHash = passwordHasher.hash(command.newPassword());

    User updatedUser = user.resetPassword(newPasswordHash, now);

    userRepository.updatePassword(updatedUser);

    passwordResetTokenRepository.markUsed(consumedToken);

    passwordResetTokenRepository.revokeOtherUnusedByUserId(user.id(), consumedToken.id());

    refreshTokenRepository.revokeAllByUserId(user.id());

    return new ResetPasswordResult(true);
  }
}
