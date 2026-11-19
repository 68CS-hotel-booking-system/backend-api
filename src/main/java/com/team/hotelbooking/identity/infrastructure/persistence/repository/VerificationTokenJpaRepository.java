package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenJpaRepository
    extends JpaRepository<VerificationTokenJpaEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VerificationTokenJpaEntity> findByTokenHash(String tokenHash);

  @Query("select t.userId from VerificationTokenJpaEntity t where t.tokenHash = :tokenHash")
  Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

  @Query("select max(t.createdAt) from VerificationTokenJpaEntity t where t.userId = :userId")
  Optional<Instant> findLatestCreatedAtByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update VerificationTokenJpaEntity t
      set t.revokedAt = :revokedAt
      where t.userId = :userId
          and t.usedAt is null
          and t.revokedAt is null
      """)
  int revokeAllUnusedByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
