package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UUID> {

  @Query(
      """
      select r.roleKey
      from RoleJpaEntity r, UserRoleJpaEntity  ur
      where ur.roleId = r.id
           and ur.userId = :userId
           and r.active = true
      """)
  Set<String> findRoleKeysByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true)
  @Query("delete from UserRoleJpaEntity ur where ur.userId = :userId and ur.roleId = :roleId")
  void deleteAssignment(@Param("userId") UUID userId, @Param("roleId") Integer roleId);

  @Query(
      """
      select count(ur)
      from UserRoleJpaEntity ur, UserJpaEntity u
      where ur.userId = u.id
          and ur.roleId = :roleId
          and u.deleted = false
          and u.status = :status
          and u.emailVerifiedAt is not null
      """)
  long countActiveUsersByRoleId(
      @Param("roleId") Integer roleId, @Param("status") UserJpaEntity.DbStatus status);

  interface UserRoleKeyView {

    UUID getUserId();

    String getRoleKey();
  }

  @Query(
      """
      select ur.userId as userId, r.roleKey as roleKey
      from UserRoleJpaEntity ur, RoleJpaEntity r
      where ur.roleId = r.id
          and ur.userId in :userIds
          and r.active = true
      """)
  List<UserRoleKeyView> findRoleKeysByUserIds(@Param("userIds") List<UUID> userIds);
}
