package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.ResendUserVerificationEmailCommand;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import com.team.hotelbooking.identity.application.result.ResendUserVerificationEmailResult;
import com.team.hotelbooking.identity.application.usecase.ResendUserVerificationEmailUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.VerificationToken;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class ResendUserVerificationEmailService
    implements ResendUserVerificationEmailUseCase {

  private final UserRepository users;
  private final VerificationTokenRepository tokens;
  private final OpaqueTokens opaqueTokens;
  private final VerificationEmailQueue emails;
  private final Clock clock;
  private final Duration verificationTtl;
  private final Duration requestCooldown;

  public ResendUserVerificationEmailService(
      UserRepository users,
      VerificationTokenRepository tokens,
      OpaqueTokens opaqueTokens,
      VerificationEmailQueue emails,
      Clock clock,
      Duration verificationTtl,
      Duration requestCooldown) {
    if (verificationTtl.isZero()
        || verificationTtl.isNegative()
        || requestCooldown.isZero()
        || requestCooldown.isNegative()) {
      throw new IllegalArgumentException("Verification TTL and cooldown must be positive.");
    }
    this.users = users;
    this.tokens = tokens;
    this.opaqueTokens = opaqueTokens;
    this.emails = emails;
    this.clock = clock;
    this.verificationTtl = verificationTtl;
    this.requestCooldown = requestCooldown;
  }

  @Override
  public ResendUserVerificationEmailResult execute(ResendUserVerificationEmailCommand command) {
    if (command == null || command.actorId() == null || command.userId() == null) {
      throw IdentityException.invalid("Thông tin người thực hiện và tài khoản đích là bắt buộc.");
    }
    User actor =
        users
            .findById(command.actorId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.ACCESS_DENIED));
    if (!actor.roles().contains("ADMIN")) {
      throw new IdentityException(IdentityErrorCode.ACCESS_DENIED);
    }
    actor.ensureCanLogin();

    // Tuần tự hóa các yêu cầu gửi lại và dùng cùng thứ tự user -> token với luồng xác minh.
    User target =
        users
            .findByIdForUpdate(command.userId())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    if (target.status() == User.Status.BANNED || target.status() == User.Status.INACTIVE) {
      throw new IdentityException(IdentityErrorCode.ACCOUNT_LOCKED);
    }
    if (target.emailVerifiedAt() != null) {
      throw new IdentityException(IdentityErrorCode.EMAIL_ALREADY_VERIFIED);
    }
    if (target.status() != User.Status.PENDING) {
      throw new IdentityException(IdentityErrorCode.CONFLICT);
    }

    Instant now = clock.instant();
    Instant latestCreatedAt = tokens.findLatestCreatedAtByUserId(target.id()).orElse(null);
    if (latestCreatedAt != null && now.isBefore(latestCreatedAt.plus(requestCooldown))) {
      throw new IdentityException(IdentityErrorCode.RATE_LIMIT_EXCEEDED);
    }

    tokens.revokeAllUnusedByUserId(target.id(), now);
    OpaqueTokens.IssuedToken issued = opaqueTokens.generate();
    Instant expiresAt = now.plus(verificationTtl);
    tokens.add(
        new VerificationToken(UUID.randomUUID(), target.id(), issued.hash(), expiresAt, now));
    emails.enqueue(target.id(), target.email(), issued.raw(), expiresAt);

    return new ResendUserVerificationEmailResult(true);
  }
}
