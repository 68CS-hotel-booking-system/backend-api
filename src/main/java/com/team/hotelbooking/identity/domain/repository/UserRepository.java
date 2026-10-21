package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.User;

public interface UserRepository {

  boolean existsByEmail(String normalizedEmail);

  boolean existsByPhone(String normalizedPhone);

  User add(User user);
}
