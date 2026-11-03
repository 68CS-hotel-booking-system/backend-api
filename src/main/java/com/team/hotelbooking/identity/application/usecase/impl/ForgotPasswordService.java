package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordResetEmailQueue;
import com.team.hotelbooking.identity.application.result.ForgotPasswordResult;
import com.team.hotelbooking.identity.application.usecase.ForgotPasswordUseCase;
import com.team.hotelbooking.identity.domain.model.PasswordResetToken;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.PasswordResetTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class ForgotPasswordService implements ForgotPasswordUseCase {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final OpaqueTokens opaqueTokens;
  private final PasswordResetEmailQueue passwordResetEmailQueue;
  private final Clock clock;
  private final Duration resetTtl;
  private final Duration requestCooldown;

  public ForgotPasswordService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      OpaqueTokens opaqueTokens,
      PasswordResetEmailQueue passwordResetEmailQueue,
      Clock clock,
      Duration resetTtl,
      Duration requestCooldown) {

    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.opaqueTokens = opaqueTokens;
    this.passwordResetEmailQueue = passwordResetEmailQueue;
    this.clock = clock;
    this.resetTtl = resetTtl;
    this.requestCooldown = requestCooldown;
  }

  @Override
  public ForgotPasswordResult execute(String email) {

    String emailNormalized = User.normalizeEmail(email);

    User user = userRepository.findByEmail(emailNormalized).orElse(null);

    if (user == null) {
      return new ForgotPasswordResult(true);
    }

    User userLocked = userRepository.findByIdForUpdate(user.id()).orElse(null);

    if (userLocked == null) {
      return new ForgotPasswordResult(true);
    }

    if (userLocked.status() == User.Status.BANNED || userLocked.status() == User.Status.INACTIVE) {
      return new ForgotPasswordResult(true);
    }

    Instant now = clock.instant();

    Instant latestCreatedAt =
        passwordResetTokenRepository.findLatestCreatedAtByUserId(userLocked.id()).orElse(null);

    if (latestCreatedAt != null && now.isBefore(latestCreatedAt.plus(requestCooldown))) {

      return new ForgotPasswordResult(true);
    }

    var issuedToken = opaqueTokens.generate();

    Instant expiresAt = now.plus(resetTtl);

    passwordResetTokenRepository.add(
        new PasswordResetToken(
            UUID.randomUUID(), user.id(), issuedToken.hash(), expiresAt, now, null, false));

    passwordResetEmailQueue.enqueue(user.id(), user.email(), issuedToken.raw(), expiresAt);

    return new ForgotPasswordResult(true);
  }
}
