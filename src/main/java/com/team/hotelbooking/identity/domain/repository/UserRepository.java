package com.team.hotelbooking.identity.domain.repository;

import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.UserSearchCriteria;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  boolean existsByEmail(String normalizedEmail);

  boolean existsByPhone(String normalizedPhone);

  User add(User user);

  Optional<User> findByIdForUpdate(UUID id);

  void updateVerification(User user);

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String normalizedEmail);

  void updatePassword(User updatedUser);

  User updateStatus(User updatedUser, UUID actorId, String reason);

  PageResult<User> findAll(UserSearchCriteria criteria, PageQuery pagination);
}
