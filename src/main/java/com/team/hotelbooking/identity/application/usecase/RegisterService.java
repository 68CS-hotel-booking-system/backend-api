package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.RegisterCommand;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import com.team.hotelbooking.identity.application.result.RegisterResult;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.VerificationToken;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import com.team.hotelbooking.identity.domain.service.PasswordPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class RegisterService implements RegisterUseCase {

  private final UserRepository users;
  private final VerificationTokenRepository verificationTokens;
  private final PasswordHasher passwords;
  private final OpaqueTokens opaqueTokens;
  private final VerificationEmailQueue emails;
  private final Clock clock;
  private final Duration verificationTtl;

  public RegisterService(
      UserRepository users,
      VerificationTokenRepository verificationTokens,
      PasswordHasher passwords,
      OpaqueTokens opaqueTokens,
      VerificationEmailQueue emails,
      Clock clock,
      Duration verificationTtl) {

    if (verificationTtl.isZero() || verificationTtl.isNegative()) {
      throw new IllegalArgumentException("Verification TTL must be positive.");
    }

    this.users = users;
    this.verificationTokens = verificationTokens;
    this.passwords = passwords;
    this.opaqueTokens = opaqueTokens;
    this.emails = emails;
    this.clock = clock;
    this.verificationTtl = verificationTtl;
  }

  @Override
  public RegisterResult execute(RegisterCommand command) {
    PasswordPolicy.validateNewPassword(command.password());

    String email = User.normalizeEmail(command.email());
    String phone = User.normalizePhone(command.phone());

    if (users.existsByEmail(email)) {
      throw new IdentityException(IdentityErrorCode.EMAIL_ALREADY_USED);
    }

    if (users.existsByPhone(phone)) {
      throw new IdentityException(IdentityErrorCode.PHONE_ALREADY_USED);
    }

    Instant now = clock.instant();

    User user =
        User.register(
            UUID.randomUUID(),
            email,
            passwords.hash(command.password()),
            command.fullName(),
            phone,
            now);

    User saved = users.add(user);

    OpaqueTokens.IssuedToken issued = opaqueTokens.generate();
    Instant expiresAt = now.plus(verificationTtl);

    verificationTokens.add(
        new VerificationToken(UUID.randomUUID(), saved.id(), issued.hash(), expiresAt, now));

    emails.enqueue(saved.id(), saved.email(), issued.raw(), expiresAt);

    return RegisterResult.from(saved);
  }
}
