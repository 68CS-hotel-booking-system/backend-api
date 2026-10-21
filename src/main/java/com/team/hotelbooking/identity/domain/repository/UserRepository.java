package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  boolean existsByEmail(String normalizedEmail);

  boolean existsByPhone(String normalizedPhone);

  User add(User user);

  Optional<User> findByIdForUpdate(UUID id);

  void updateVerification(User user);
}
