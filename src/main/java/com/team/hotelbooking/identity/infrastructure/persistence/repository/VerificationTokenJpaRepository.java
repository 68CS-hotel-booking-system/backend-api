package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VerificationTokenJpaRepository
    extends JpaRepository<VerificationTokenJpaEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VerificationTokenJpaEntity> findByTokenHash(String tokenHash);
}
