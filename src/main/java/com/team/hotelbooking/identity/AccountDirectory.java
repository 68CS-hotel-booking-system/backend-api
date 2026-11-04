package com.team.hotelbooking.identity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountDirectory {

  Optional<AccountInfo> findById(UUID userId);

  record AccountInfo(UUID id, boolean active, boolean emailVerified, Set<String> roles) {

    public AccountInfo {
      roles = Set.copyOf(roles);
    }
  }
}
