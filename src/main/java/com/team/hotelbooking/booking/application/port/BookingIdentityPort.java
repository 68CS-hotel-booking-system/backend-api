package com.team.hotelbooking.booking.application.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookingIdentityPort {

  Optional<AccountSnapshot> findById(UUID userId);

  record AccountSnapshot(
      UUID id,
      String fullName,
      String email,
      boolean active,
      boolean emailVerified,
      Set<String> roles) {
    public AccountSnapshot {
      roles = Set.copyOf(roles);
    }
  }
}
