package com.team.hotelbooking.identity.infrastructure.persistence.adapter;

import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.identity.domain.model.User;
import com.team.hotelbooking.identity.domain.model.UserSearchCriteria;
import com.team.hotelbooking.identity.domain.repository.UserRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserStatusChangeJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.RoleJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserRoleJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserSpecifications;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserStatusChangeJpaRepository;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.persistence.PageResultMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

  private final UserJpaRepository users;
  private final RoleJpaRepository roles;
  private final UserRoleJpaRepository userRoles;
  private final UserStatusChangeJpaRepository statusChanges;

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
  public void updatePassword(User updatedUser) {
    UserJpaEntity entity =
        users
            .findByIdAndDeletedFalse(updatedUser.id())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.TOKEN_INVALID));

    entity.setPasswordHash(updatedUser.passwordHash());
    entity.setAuthVersion(updatedUser.authVersion());
    entity.setUpdatedAt(updatedUser.updatedAt());

    users.saveAndFlush(entity);
  }

  @Override
  public User updateStatus(User updatedUser, UUID actorId, String reason) {
    UserJpaEntity entity =
        users
            .findByIdAndDeletedFalse(updatedUser.id())
            .orElseThrow(() -> new IdentityException(IdentityErrorCode.USER_NOT_FOUND));
    if (entity.getVersion() != updatedUser.version()) {
      throw new IdentityException(IdentityErrorCode.STALE_USER_VERSION);
    }

    UserStatusChangeJpaEntity change = new UserStatusChangeJpaEntity();
    change.setId(UUID.randomUUID());
    change.setUserId(entity.getId());
    change.setActorId(actorId);
    change.setPreviousStatus(entity.getStatus().name());
    change.setNewStatus(updatedUser.status().name().toLowerCase(Locale.ROOT));
    change.setReason(reason == null || reason.isBlank() ? null : reason);
    change.setChangedAt(updatedUser.updatedAt());

    entity.setStatus(UserJpaEntity.DbStatus.valueOf(change.getNewStatus()));
    entity.setAuthVersion(updatedUser.authVersion());
    entity.setUpdatedAt(updatedUser.updatedAt());
    UserJpaEntity saved = users.saveAndFlush(entity);
    statusChanges.saveAndFlush(change);

    return UserPersistenceMapper.toDomain(saved, updatedUser.roles());
  }

  @Override
  public PageResult<User> findAll(UserSearchCriteria criteria, PageQuery pagination) {

    PageRequest pageable =
        PageRequest.of(pagination.page(), pagination.size(), resolveSort(criteria.sort()));

    Page<UserJpaEntity> page = users.findAll(UserSpecifications.matching(criteria), pageable);

    Map<UUID, Set<String>> rolesByUserId = new HashMap<>();

    if (page.hasContent()) {
      var userIds = page.getContent().stream().map(UserJpaEntity::getId).toList();

      var assignments = userRoles.findRoleKeysByUserIds(userIds);

      for (var assignment : assignments) {
        rolesByUserId
            .computeIfAbsent(assignment.getUserId(), ignored -> new HashSet<>())
            .add(assignment.getRoleKey());
      }
    }

    return PageResultMapper.from(
        page,
        entity ->
            UserPersistenceMapper.toDomain(
                entity, rolesByUserId.getOrDefault(entity.getId(), Set.of())));
  }

  private static Sort resolveSort(String rawSort) {
    String value = rawSort == null || rawSort.isEmpty() ? "createdAt,desc" : rawSort;

    if (!value.matches("(createdAt|fullName|email),(asc|desc)")) {
      throw IdentityException.invalid(
          "Sắp xếp theo createdAt, fullName hoặc email với asc hoặc desc.");
    }

    String[] parts = value.split(",");

    Sort.Direction direction = "asc".equals(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;

    return Sort.by(new Sort.Order(direction, parts[0]), new Sort.Order(direction, "id"));
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
