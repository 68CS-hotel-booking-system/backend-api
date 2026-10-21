package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);
}
