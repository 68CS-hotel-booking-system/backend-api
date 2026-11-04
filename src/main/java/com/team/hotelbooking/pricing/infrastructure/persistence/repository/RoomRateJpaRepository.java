package com.team.hotelbooking.pricing.infrastructure.persistence.repository;

import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateIdEmbeddable;
import com.team.hotelbooking.pricing.infrastructure.persistence.entity.RoomRateJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRateJpaRepository
    extends JpaRepository<RoomRateJpaEntity, RoomRateIdEmbeddable> {

  @Query(
      """
      SELECT rr FROM RoomRateJpaEntity rr
      WHERE rr.id.roomTypeId = :roomTypeId
        AND rr.id.rateDate >= :startDate
        AND rr.id.rateDate <= :endDate
      ORDER BY rr.id.rateDate ASC
      """)
  List<RoomRateJpaEntity> findRatesByRoomTypeIdAndDateRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      """
      SELECT rr FROM RoomRateJpaEntity rr
      WHERE rr.id.roomTypeId = :roomTypeId
        AND rr.id.rateDate = :rateDate
      """)
  Optional<RoomRateJpaEntity> findByRoomTypeIdAndRateDate(
      @Param("roomTypeId") UUID roomTypeId, @Param("rateDate") LocalDate rateDate);
}
