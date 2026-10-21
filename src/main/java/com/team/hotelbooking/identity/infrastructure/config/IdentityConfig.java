package com.team.hotelbooking.identity.infrastructure.config;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import com.team.hotelbooking.identity.application.usecase.impl.RegisterService;
import com.team.hotelbooking.identity.application.usecase.RegisterUseCase;
import com.team.hotelbooking.identity.application.usecase.impl.VerifyEmailService;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class IdentityConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public PasswordHasher passwordHasher() {
    PasswordEncoder pbkdf2 =
      new Pbkdf2PasswordEncoder(
        "", 16, 600_000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);

    PasswordEncoder encoder =
      new DelegatingPasswordEncoder("pbkdf2-v1", Map.of("pbkdf2-v1", pbkdf2));

    return new PasswordHasher() {
      @Override
      public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
      }

      @Override
      public boolean matches(String rawPassword, String encodedPassword) {

        return encoder.matches(rawPassword, encodedPassword);
      }
    };
  }

  @Bean
  public RegisterUseCase registerUseCase(
    UserRepository users,
    VerificationTokenRepository tokens,
    PasswordHasher passwords,
    OpaqueTokens opaqueTokens,
    VerificationEmailQueue emails,
    Clock clock,
    PlatformTransactionManager transactionManager,
    @Value("${auth.verification-ttl}") String ttl) {

    RegisterService service =
      new RegisterService(
        users, tokens, passwords, opaqueTokens, emails, clock, Duration.parse(ttl));

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
      Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public VerifyEmailUseCase verifyEmailUseCase(
    UserRepository users,
    VerificationTokenRepository tokens,
    OpaqueTokens opaqueTokens,
    Clock clock,
    PlatformTransactionManager transactionManager) {

    VerifyEmailService service = new VerifyEmailService(users, tokens, opaqueTokens, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return rawToken ->
      Objects.requireNonNull(transaction.execute(status -> service.execute(rawToken)));
  }
}
