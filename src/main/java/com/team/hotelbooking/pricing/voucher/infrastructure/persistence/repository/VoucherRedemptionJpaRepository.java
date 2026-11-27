package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.repository;

import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity.VoucherRedemptionJpaEntity;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VoucherRedemptionJpaRepository
    extends JpaRepository<VoucherRedemptionJpaEntity, UUID> {
  @Query(
      "select count(r) from VoucherRedemptionJpaEntity r where r.voucherId=:id and (r.status='USED' or (r.status='RESERVED' and r.expiresAt>:now))")
  long occupied(@Param("id") UUID id, @Param("now") Instant now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update VoucherRedemptionJpaEntity r set r.status='USED' where r.bookingId=:id and r.status='RESERVED'")
  int consume(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update VoucherRedemptionJpaEntity r set r.status='RELEASED' where r.bookingId=:id and r.status='RESERVED'")
  int release(@Param("id") UUID id);
}
