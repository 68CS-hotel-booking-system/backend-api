package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  @Query(
      """
          select t.userId
          from RefreshTokenJpaEntity t
          where t.tokenHash = :tokenHash
      """)
  Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

  @Query(
      """
      select t
      from RefreshTokenJpaEntity t
      where t.tokenHash = :tokenHash
      """)
  Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update RefreshTokenJpaEntity t
      set t.revoked = true
      where t.familyId = :familyId
      and t.revoked = false
      """)
  void revokeByFamilyId(@Param("familyId") UUID family);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update RefreshTokenJpaEntity  t
      set t.revoked = true
      where t.id = :tokenId
      """)
  void revokeByTokenId(@Param("tokenId") UUID tokenId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
    update RefreshTokenJpaEntity t
    set t.revoked = true
    where t.userId = :userId
      and t.revoked = false
    """)
  int revokeAllByUserId(@Param("userId") UUID userId);
}
