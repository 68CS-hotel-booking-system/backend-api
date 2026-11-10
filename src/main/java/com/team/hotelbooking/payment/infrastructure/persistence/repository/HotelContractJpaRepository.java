package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
        AND c.status = :status
        AND c.startsAt < :endsAt
        AND c.endsAt > :startsAt
      """)
  long countOverlappingActiveContracts(
      @Param("hotelId") UUID hotelId,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt,
      @Param("status") HotelContractJpaEntity.DbContractStatus status);

  default long countOverlappingActiveContracts(UUID hotelId, Instant startsAt, Instant endsAt) {
    return countOverlappingActiveContracts(
        hotelId, startsAt, endsAt, HotelContractJpaEntity.DbContractStatus.ACTIVE);
  }

  // Như countOverlappingActiveContracts nhưng bỏ qua chính hợp đồng đang kích hoạt.
  @Query(
      """
      SELECT COUNT(c) FROM HotelContractJpaEntity c
      WHERE c.hotelId = :hotelId
        AND c.id <> :excludedId
        AND c.status = :status
        AND c.startsAt < :endsAt
        AND c.endsAt > :startsAt
      """)
  long countOverlappingActiveContractsExcept(
      @Param("hotelId") UUID hotelId,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt,
      @Param("excludedId") UUID excludedId,
      @Param("status") HotelContractJpaEntity.DbContractStatus status);

  default long countOverlappingActiveContractsExcluding(
      UUID hotelId, Instant startsAt, Instant endsAt, UUID excludedId) {
    if (excludedId == null) {
      return countOverlappingActiveContracts(hotelId, startsAt, endsAt);
    }
    return countOverlappingActiveContractsExcept(
        hotelId, startsAt, endsAt, excludedId, HotelContractJpaEntity.DbContractStatus.ACTIVE);
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM HotelContractJpaEntity c WHERE c.id = :id AND c.hotelId = :hotelId")
  Optional<HotelContractJpaEntity> findByIdAndHotelIdForUpdate(
      @Param("id") UUID id, @Param("hotelId") UUID hotelId);

  // Hiệu lực trong [starts_at, ends_at); ràng buộc exclusion bảo đảm tối đa một hợp đồng khớp.
  @Query(
      """
      SELECT c FROM HotelContractJpaEntity c
      WHERE c.hotelId = :hotelId
        AND c.status = :status
        AND c.startsAt <= :at
        AND c.endsAt > :at
      ORDER BY c.startsAt DESC
      """)
  List<HotelContractJpaEntity> findEffectiveActive(
      @Param("hotelId") UUID hotelId,
      @Param("at") Instant at,
      @Param("status") HotelContractJpaEntity.DbContractStatus status,
      Pageable pageable);

  default List<HotelContractJpaEntity> findEffectiveActive(
      UUID hotelId, Instant at, Pageable pageable) {
    return findEffectiveActive(
        hotelId, at, HotelContractJpaEntity.DbContractStatus.ACTIVE, pageable);
  }
}
