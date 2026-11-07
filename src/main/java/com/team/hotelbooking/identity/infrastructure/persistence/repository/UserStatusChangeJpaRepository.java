package com.team.hotelbooking.identity.infrastructure.persistence.repository;

import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserStatusChangeJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStatusChangeJpaRepository
    extends JpaRepository<UserStatusChangeJpaEntity, UUID> {}
