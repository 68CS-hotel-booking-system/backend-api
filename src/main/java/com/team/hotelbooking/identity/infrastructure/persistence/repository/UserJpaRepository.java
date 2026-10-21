package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<UserJpaEntity> findByIdAndDeletedFalse(UUID id);
}
