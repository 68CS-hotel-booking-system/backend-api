package com.team.hotelbooking.hotel.application.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IdentityAccounts {

  Optional<AccountSnapshot> findById(UUID userId);

  record AccountSnapshot(UUID id, boolean active, boolean emailVerified, Set<String> roles) {

    public AccountSnapshot {
      roles = Set.copyOf(roles);
    }
  }
}
