package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelContractJpaRepository extends JpaRepository<HotelContractJpaEntity, UUID> {

  boolean existsByContractCode(String contractCode);

  Optional<HotelContractJpaEntity> findByContractCode(String contractCode);

  Page<HotelContractJpaEntity> findAllByHotelId(UUID hotelId, Pageable pageable);

  Page<HotelContractJpaEntity> findAllByHotelIdAndStatus(
      UUID hotelId, HotelContractJpaEntity.DbContractStatus status, Pageable pageable);

  @Query(
      """
      SELECT COUNT(c) FROM HotelContractJpaEntity c
      WHERE c.hotelId = :hotelId
        AND c.status = com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity.DbContractStatus.ACTIVE
        AND c.startsAt < :endsAt
        AND c.endsAt > :startsAt
      """)
  long countOverlappingActiveContracts(
      @Param("hotelId") UUID hotelId,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt);
}
