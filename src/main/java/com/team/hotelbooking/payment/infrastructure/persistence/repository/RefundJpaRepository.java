package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.domain.model.Refund;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.RefundJpaEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefundJpaRepository
    extends JpaRepository<RefundJpaEntity, UUID>, JpaSpecificationExecutor<RefundJpaEntity> {
  Optional<RefundJpaEntity> findByPaymentId(UUID id);

  List<RefundJpaEntity> findByBookingIdOrderByCreatedAtAscIdAsc(UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE RefundJpaEntity r SET r.status=:status,r.settlementReference=:reference,r.note=:note,r.updatedAt=:now,r.completedAt=:completed,r.version=r.version+1 WHERE r.id=:id AND r.version=:version")
  int update(
      @Param("id") UUID id,
      @Param("version") long version,
      @Param("status") Refund.Status status,
      @Param("reference") String reference,
      @Param("note") String note,
      @Param("now") Instant now,
      @Param("completed") Instant completed);
}
