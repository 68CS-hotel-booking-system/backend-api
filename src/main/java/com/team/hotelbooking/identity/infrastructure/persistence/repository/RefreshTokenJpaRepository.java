package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {}
