package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Integer> {

  Optional<RoleJpaEntity> findByRoleKeyAndActiveTrue(String roleKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RoleJpaEntity r where r.id = :roleId")
  Optional<RoleJpaEntity> findByIdForUpdate(@Param("roleId") Integer roleId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from RoleJpaEntity r where r.roleKey = :roleKey")
  Optional<RoleJpaEntity> findByRoleKeyForUpdate(@Param("roleKey") String roleKey);

  List<RoleJpaEntity> findAllByActiveTrueOrderByRoleKeyAsc();
}
