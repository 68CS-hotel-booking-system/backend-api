package com.team.hotelbooking.identity.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleIdentityRepository {
  Optional<UUID> findUserId(String subject);

  boolean add(String subject, UUID userId);
}
