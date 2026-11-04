package com.team.hotelbooking.payment.application.port;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdentityPort {

  record UserSnapshot(UUID id, String fullName, String email) {}

  boolean existsById(UUID userId);

  Optional<UserSnapshot> findUserSnapshot(UUID userId);
}
