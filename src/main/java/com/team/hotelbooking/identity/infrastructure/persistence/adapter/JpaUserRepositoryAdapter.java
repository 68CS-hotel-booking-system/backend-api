package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.RoleJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository users;
  private final RoleJpaRepository roles;
  private final UserRoleJpaRepository userRoles;

  @Override
  public boolean existsByEmail(String normalizedEmail) {
    return users.existsByEmail(normalizedEmail);
  }

  @Override
  public boolean existsByPhone(String normalizedPhone) {
    return users.existsByPhone(normalizedPhone);
  }

  @Override
  public User add(User user) {
    UserJpaEntity entity = UserPersistenceMapper.toNewEntity(user);

    try {
      entity = users.saveAndFlush(entity);
    } catch (DataIntegrityViolationException exception) {
      throw translateConflict(exception);
    }

    for (String roleKey : user.roles()) {
      RoleJpaEntity role =
          roles
              .findByRoleKeyAndActiveTrue(roleKey)
              .orElseThrow(
                  () -> new IllegalStateException("Required active role is missing: " + roleKey));

      UserRoleJpaEntity assignment = new UserRoleJpaEntity();
      assignment.setUserId(entity.getId());
      assignment.setRoleId(role.getId());
      assignment.setAssignedAt(user.createdAt());

      userRoles.saveAndFlush(assignment);
    }

    return UserPersistenceMapper.toDomain(entity, user.roles());
  }

  @Override
  public Optional<User> findByIdForUpdate(UUID id) {
    return users
        .findByIdAndDeletedFalse(id)
        .map(entity -> UserPersistenceMapper.toDomain(entity, userRoles.findRoleKeysByUserId(id)));
  }

  @Override
  public void updateVerification(User user) {
    UserJpaEntity entity =
        users
            .findByIdAndDeletedFalse(user.id())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    entity.setStatus(UserJpaEntity.DbStatus.valueOf(user.status().name().toLowerCase(Locale.ROOT)));

    entity.setEmailVerifiedAt(user.emailVerifiedAt());
    entity.setUpdatedAt(user.updatedAt());

    users.saveAndFlush(entity);
  }

  @Override
  public Optional<User> findByEmail(String normalizedEmail) {
    return users
        .findByEmailAndDeletedFalse(normalizedEmail)
        .map(
            entity ->
                UserPersistenceMapper.toDomain(
                    entity, userRoles.findRoleKeysByUserId(entity.getId())));
  }

  @Override
  public Optional<User> findById(UUID id) {
    return users
        .findOneByIdAndDeletedFalse(id)
        .map(entity -> UserPersistenceMapper.toDomain(entity, userRoles.findRoleKeysByUserId(id)));
  }

  private RuntimeException translateConflict(DataIntegrityViolationException exception) {

    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {

      if (cause instanceof ConstraintViolationException violation
          && "23505".equals(violation.getSQLException().getSQLState())) {

        String constraint = violation.getConstraintName();

        if ("uq_users_email".equals(constraint) || "users_email_key".equals(constraint)) {

          return new IdentityException(IdentityErrorCode.EMAIL_ALREADY_USED);
        }

        if ("uq_users_phone".equals(constraint) || "users_phone_key".equals(constraint)) {

          return new IdentityException(IdentityErrorCode.PHONE_ALREADY_USED);
        }
      }
    }

    return exception;
  }
}
