package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationJpaRepository
    extends JpaRepository<InventoryReservationJpaEntity, UUID> {

  @Query(
      """
      SELECT COALESCE(SUM(ir.quantity), 0)
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate = :date
        AND ir.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus.HELD
        AND ir.expiresAt > :now
      """)
  long countHeldRoomsOnDate(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("date") LocalDate date,
      @Param("now") Instant now);

  @Query(
      """
      SELECT COALESCE(SUM(ir.quantity), 0)
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate = :date
        AND ir.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus.CONFIRMED
      """)
  long countConfirmedRoomsOnDate(
      @Param("roomTypeId") UUID roomTypeId, @Param("date") LocalDate date);

  @Query(
      """
      SELECT ir.stayDate AS stayDate, COALESCE(SUM(ir.quantity), 0) AS totalQuantity
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate >= :startDate
        AND ir.stayDate < :endDate
        AND ((ir.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus.HELD AND ir.expiresAt > :now)
             OR ir.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus.CONFIRMED)
      GROUP BY ir.stayDate
      """)
  List<DailyReservationOccupancy> sumOccupancyByDateRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("now") Instant now);
}
