package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserRoleJpaEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UUID> {

  @Query(
      """
    select r.roleKey
    from RoleJpaEntity r, UserRoleJpaEntity  ur
    where ur.roleId = r.id and ur.userId = :userId
    """)
  Set<String> findRoleKeysByUserId(@Param("userId") UUID userId);
}
