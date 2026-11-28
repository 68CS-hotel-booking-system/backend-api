package com.team.hotelbooking.hotel.application.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityAccounts {

  Optional<AccountSnapshot> findById(UUID userId);

  default Optional<AccountSnapshot> findByEmail(String email) {
    return Optional.empty();
  }

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

    public AccountSnapshot(UUID id, boolean active, boolean emailVerified, Set<String> roles) {
      this(id, null, null, active, emailVerified, roles);
    }
  }
}
