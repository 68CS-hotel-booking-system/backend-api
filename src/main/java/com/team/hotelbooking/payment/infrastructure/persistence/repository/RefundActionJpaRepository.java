package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.RefundActionJpaEntity;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundActionJpaRepository extends JpaRepository<RefundActionJpaEntity, UUID> {
  List<RefundActionJpaEntity> findByRefundIdOrderByVersionAsc(UUID id);
}
