package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentIdentityAdapter implements PaymentIdentityPort {

  private final UserJpaRepository userJpaRepository;

  @Override
  public boolean existsById(UUID userId) {
    return userJpaRepository.existsById(userId);
  }

  @Override
  public Optional<UserSnapshot> findUserSnapshot(UUID userId) {
    return userJpaRepository
        .findById(userId)
        .map(u -> new UserSnapshot(u.getId(), u.getFullName(), u.getEmail()));
  }
}
