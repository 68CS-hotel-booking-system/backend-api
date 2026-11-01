package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.RoleJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Integer> {

  Optional<RoleJpaEntity> findByRoleKeyAndActiveTrue(String roleKey);
}
