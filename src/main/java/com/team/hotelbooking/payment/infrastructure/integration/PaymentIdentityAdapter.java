package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.identity.AccountDirectory;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentIdentityAdapter implements PaymentIdentityPort {

  private final AccountDirectory accountDirectory;

  @Override
  public boolean existsById(UUID userId) {
    return accountDirectory.existsById(userId);
  }

  @Override
  public Optional<UserSnapshot> findUserSnapshot(UUID userId) {
    return accountDirectory
        .findById(userId)
        .map(u -> new UserSnapshot(u.id(), u.fullName(), u.email()));
  }
}
