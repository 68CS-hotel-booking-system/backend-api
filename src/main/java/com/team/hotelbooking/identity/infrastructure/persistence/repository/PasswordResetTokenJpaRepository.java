package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenJpaRepository
    extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

  @Query(
      """
      select max(t.createdAt)
      from PasswordResetTokenJpaEntity t
      where t.userId = :userId
      """)
  Optional<Instant> findLatestCreatedAtByUserId(@Param("userId") UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      """
      update PasswordResetTokenJpaEntity t
      set t.usedAt = :usedAt
      where t.id = :tokenId
        and t.usedAt is null
        and t.revoked = false
      """)
  int markUsed(@Param("tokenId") UUID tokenId, @Param("usedAt") Instant usedAt);

  @Modifying
  @Query(
      """
      update PasswordResetTokenJpaEntity t
      set t.revoked = true
      where t.userId = :userId
        and t.id <> :excludedTokenId
        and t.usedAt is null
        and t.revoked = false
      """)
  int revokeOtherUnusedByUserId(
      @Param("userId") UUID userId, @Param("excludedTokenId") UUID excludedTokenId);

  @Query(
      """
      select t.userId
      from PasswordResetTokenJpaEntity t
      where t.tokenHash = :tokenHash
      """)
  Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query(
      """
    update PasswordResetTokenJpaEntity t
    set t.revoked = true
    where t.userId = :userId
        and t.usedAt is null
        and t.revoked = false
    """)
  int revokeAllUnusedByUserId(@Param("userId") UUID userId);
}
