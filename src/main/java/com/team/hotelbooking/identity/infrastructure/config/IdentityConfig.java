package com.team.hotelbooking.identity.infrastructure.config;

import com.team.hotelbooking.identity.application.port.AccessTokens;
import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.port.PasswordResetEmailQueue;
import com.team.hotelbooking.identity.application.port.VerificationEmailQueue;
import com.team.hotelbooking.identity.application.result.LoginResult;
import com.team.hotelbooking.identity.application.usecase.ForgotPasswordUseCase;
import com.team.hotelbooking.identity.application.usecase.GetCurrentUserUseCase;
import com.team.hotelbooking.identity.application.usecase.LoginUseCase;
import com.team.hotelbooking.identity.application.usecase.LogoutUseCase;
import com.team.hotelbooking.identity.application.usecase.RefreshTokenUseCase;
import com.team.hotelbooking.identity.application.usecase.RegisterUseCase;
import com.team.hotelbooking.identity.application.usecase.ResetPasswordUseCase;
import com.team.hotelbooking.identity.application.usecase.VerifyEmailUseCase;
import com.team.hotelbooking.identity.application.usecase.impl.ForgotPasswordService;
import com.team.hotelbooking.identity.application.usecase.impl.GetCurrentUserService;
import com.team.hotelbooking.identity.application.usecase.impl.LoginService;
import com.team.hotelbooking.identity.application.usecase.impl.LogoutService;
import com.team.hotelbooking.identity.application.usecase.impl.RefreshTokenService;
import com.team.hotelbooking.identity.application.usecase.impl.RegisterService;
import com.team.hotelbooking.identity.application.usecase.impl.ResetPasswordService;
import com.team.hotelbooking.identity.application.usecase.impl.VerifyEmailService;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.repository.PasswordResetTokenRepository;
import com.team.hotelbooking.identity.domain.repository.RefreshTokenRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.domain.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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

  @Bean
  public LoginUseCase loginUseCase(
      UserRepository users,
      PasswordHasher passwords,
      AccessTokens accessTokens,
      OpaqueTokens opaqueTokens,
      RefreshTokenRepository refreshTokens,
      Clock clock,
      PlatformTransactionManager transactionManager,
      @Value("${auth.refresh-ttl}") String refreshTtl) {

    String dummyHash = passwords.hash(UUID.randomUUID().toString());

    LoginService service =
        new LoginService(
            users,
            passwords,
            accessTokens,
            opaqueTokens,
            refreshTokens,
            clock,
            Duration.parse(refreshTtl),
            dummyHash);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public RefreshTokenUseCase refreshTokenUseCase(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      OpaqueTokens opaqueTokens,
      AccessTokens accessTokens,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    RefreshTokenService service =
        new RefreshTokenService(users, refreshTokens, opaqueTokens, accessTokens, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return rawToken -> {
      RefreshExecution execution =
          Objects.requireNonNull(
              transaction.execute(
                  status -> {
                    try {
                      LoginResult result = service.execute(rawToken);

                      return new RefreshExecution(result, null);
                    } catch (IdentityException error) {
                      if (error.code() == IdentityErrorCode.REFRESH_TOKEN_REUSED) {

                        // Không ném lỗi khỏi callback,
                        // để transaction commit việc thu hồi family.
                        return new RefreshExecution(null, error);
                      }

                      // Các lỗi khác vẫn làm transaction rollback.
                      throw error;
                    }
                  }));

      // Trong luồng API hiện tại, transaction đã commit tại đây.
      if (execution.error() != null) {
        throw execution.error();
      }

      return execution.result();
    };
  }

  private record RefreshExecution(LoginResult result, IdentityException error) {}

  @Bean
  public LogoutUseCase logoutUseCase(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokens,
      OpaqueTokens opaqueTokens,
      PlatformTransactionManager transactionManager) {
    LogoutService service = new LogoutService(userRepository, refreshTokens, opaqueTokens);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return rawRefreshToken ->
        transaction.executeWithoutResult(transactionStatus -> service.execute(rawRefreshToken));
  }

  @Bean
  public GetCurrentUserUseCase getCurrentUserUseCase(
      UserRepository userRepository, PlatformTransactionManager transactionManager) {
    GetCurrentUserService service = new GetCurrentUserService(userRepository);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    transaction.setReadOnly(true);

    return userId -> Objects.requireNonNull(transaction.execute(status -> service.execute(userId)));
  }

  @Bean
  public ForgotPasswordUseCase forgotPasswordUseCase(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      OpaqueTokens opaqueTokens,
      PasswordResetEmailQueue passwordResetEmailQueue,
      Clock clock,
      PlatformTransactionManager transactionManager,
      @Value("${auth.password-reset-ttl}") String resetTtl,
      @Value("${auth.password-reset-cooldown}") String requestCooldown) {

    ForgotPasswordService service =
        new ForgotPasswordService(
            userRepository,
            passwordResetTokenRepository,
            opaqueTokens,
            passwordResetEmailQueue,
            clock,
            Duration.parse(resetTtl),
            Duration.parse(requestCooldown));

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return email -> Objects.requireNonNull(transaction.execute(status -> service.execute(email)));
  }

  @Bean
  public ResetPasswordUseCase resetPasswordUseCase(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      RefreshTokenRepository refreshTokenRepository,
      OpaqueTokens opaqueTokens,
      PasswordHasher passwordHasher,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    ResetPasswordService service =
        new ResetPasswordService(
            userRepository,
            passwordResetTokenRepository,
            refreshTokenRepository,
            opaqueTokens,
            passwordHasher,
            clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}
