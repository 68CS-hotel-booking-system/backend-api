package com.team.hotelbooking.identity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountDirectory {

  Optional<AccountInfo> findById(UUID userId);

  default boolean existsById(UUID userId) {
    return findById(userId).isPresent();
  }

  record AccountInfo(
      UUID id,
      String fullName,
      String email,
      boolean active,
      boolean emailVerified,
      Set<String> roles) {

    public AccountInfo {
      roles = Set.copyOf(roles);
    }
  }
}
