package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenJpaRepository
    extends JpaRepository<VerificationTokenJpaEntity, UUID> {}
