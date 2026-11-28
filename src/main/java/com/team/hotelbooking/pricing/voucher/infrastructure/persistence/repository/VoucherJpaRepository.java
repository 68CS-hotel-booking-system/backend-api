package com.team.hotelbooking.pricing.voucher.infrastructure.persistence.repository;

import com.team.hotelbooking.pricing.voucher.infrastructure.persistence.entity.VoucherJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VoucherJpaRepository extends JpaRepository<VoucherJpaEntity, UUID> {
  Optional<VoucherJpaEntity> findByHotelIdAndCode(UUID hotelId, String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from VoucherJpaEntity v where v.hotelId=:hotel and v.code=:code")
  Optional<VoucherJpaEntity> lockCode(@Param("hotel") UUID hotel, @Param("code") String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from VoucherJpaEntity v where v.id=:id")
  Optional<VoucherJpaEntity> lockId(@Param("id") UUID id);

  Page<VoucherJpaEntity> findByHotelId(UUID hotel, Pageable page);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update VoucherJpaEntity v set v.enabled=:enabled,v.version=v.version+1 where v.id=:id and v.version=:version")
  int setEnabled(
      @Param("id") UUID id, @Param("version") long version, @Param("enabled") boolean enabled);
}
