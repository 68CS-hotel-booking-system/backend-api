package com.team.hotelbooking.identity.application.usecase.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team.hotelbooking.identity.application.port.OpaqueTokens;
import com.team.hotelbooking.identity.application.port.PasswordHasher;
import com.team.hotelbooking.identity.application.usecase.impl.GoogleAccountService.VerifiedAccount;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.GoogleIdentityRepository;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoogleAccountServiceTests {
  private static final Instant NOW = Instant.parse("2026-11-18T07:30:00Z");
  private static final VerifiedAccount GOOGLE =
      new VerifiedAccount("subject", "guest@gmail.com", "Khách thử", true);
  private final GoogleIdentityRepository identities = mock(GoogleIdentityRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final OpaqueTokens tokens = mock(OpaqueTokens.class);
  private final PasswordHasher passwords = mock(PasswordHasher.class);
  private final LoginSessionIssuer sessions = mock(LoginSessionIssuer.class);
  private final GoogleAccountService service =
      new GoogleAccountService(
          identities, users, tokens, passwords, sessions, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void enrollsVerifiedCustomerWithPhoneBeforeIssuingSession() {
    assertThatThrownBy(() -> service.signIn(GOOGLE, null))
        .isInstanceOfSatisfying(
            IdentityException.class,
            error -> assertThat(error.code()).isEqualTo(IdentityErrorCode.GOOGLE_PHONE_REQUIRED));
    verifyNoInteractions(sessions);
    when(tokens.generate()).thenReturn(new OpaqueTokens.IssuedToken("random", "token-hash"));
    when(passwords.hash("random")).thenReturn("password-hash");
    when(users.add(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(identities.add(eq(GOOGLE.subject()), any(UUID.class))).thenReturn(true);
    service.signIn(GOOGLE, "0912 345 678");

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).add(saved.capture());
    User user = saved.getValue();
    assertThat(user.phone()).isEqualTo("+84912345678");
    assertThat(user.status()).isEqualTo(User.Status.ACTIVE);
    assertThat(user.emailVerifiedAt()).isEqualTo(NOW);
    assertThat(user.roles()).containsExactly("CUSTOMER");
    verify(identities).add(GOOGLE.subject(), user.id());
    verify(sessions).issue(user, false);
  }

  @Test
  void doesNotAttachGoogleToAnExistingEmail() {
    when(users.existsByEmail(GOOGLE.email())).thenReturn(true);
    assertThatThrownBy(() -> service.signIn(GOOGLE, null))
        .isInstanceOfSatisfying(
            IdentityException.class,
            error -> assertThat(error.code()).isEqualTo(IdentityErrorCode.EMAIL_ALREADY_USED));
    verifyNoInteractions(sessions, tokens);
  }

  @Test
  void resolvesReturningAccountByGoogleSubject() {
    User saved =
        User.register(
                UUID.randomUUID(), "previous@gmail.com", "hash", "Khách thử", "0912345678", NOW)
            .verifyEmail(NOW);
    when(identities.findUserId(GOOGLE.subject())).thenReturn(Optional.of(saved.id()));
    when(users.findByIdForUpdate(saved.id())).thenReturn(Optional.of(saved));
    service.signIn(GOOGLE, null);
    verify(sessions).issue(saved, false);
    verifyNoInteractions(tokens, passwords);
  }
}
