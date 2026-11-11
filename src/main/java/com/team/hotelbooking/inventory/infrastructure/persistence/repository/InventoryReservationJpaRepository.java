package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
        AND ir.status = :status
        AND ir.expiresAt > :now
      """)
  long countHeldRoomsOnDate(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("date") LocalDate date,
      @Param("now") Instant now,
      @Param("status") DbReservationStatus status);

  default long countHeldRoomsOnDate(UUID roomTypeId, LocalDate date, Instant now) {
    return countHeldRoomsOnDate(roomTypeId, date, now, DbReservationStatus.HELD);
  }

  @Query(
      """
      SELECT COALESCE(SUM(ir.quantity), 0)
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate = :date
        AND ir.status = :status
      """)
  long countConfirmedRoomsOnDate(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("date") LocalDate date,
      @Param("status") DbReservationStatus status);

  default long countConfirmedRoomsOnDate(UUID roomTypeId, LocalDate date) {
    return countConfirmedRoomsOnDate(roomTypeId, date, DbReservationStatus.CONFIRMED);
  }

  @Query(
      """
      SELECT ir.stayDate AS stayDate, COALESCE(SUM(ir.quantity), 0) AS totalQuantity
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate >= :startDate
        AND ir.stayDate < :endDate
        AND ((ir.status = :heldStatus AND ir.expiresAt > :now)
             OR ir.status = :confirmedStatus)
      GROUP BY ir.stayDate
      """)
  List<DailyReservationOccupancy> sumOccupancyByDateRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("now") Instant now,
      @Param("heldStatus") DbReservationStatus heldStatus,
      @Param("confirmedStatus") DbReservationStatus confirmedStatus);

  default List<DailyReservationOccupancy> sumOccupancyByDateRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate, Instant now) {
    return sumOccupancyByDateRange(
        roomTypeId,
        startDate,
        endDate,
        now,
        DbReservationStatus.HELD,
        DbReservationStatus.CONFIRMED);
  }

  @Query(
      """
      SELECT ir FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate >= :startDate
        AND ir.stayDate < :endDate
        AND ((ir.status = :heldStatus AND ir.expiresAt > :now)
             OR ir.status = :confirmedStatus)
      ORDER BY ir.bookingItemId ASC NULLS LAST, ir.stayDate ASC
      """)
  List<InventoryReservationJpaEntity> findActiveReservationsInRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("now") Instant now,
      @Param("heldStatus") DbReservationStatus heldStatus,
      @Param("confirmedStatus") DbReservationStatus confirmedStatus);

  default List<InventoryReservationJpaEntity> findActiveReservationsInRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate, Instant now) {
    return findActiveReservationsInRange(
        roomTypeId,
        startDate,
        endDate,
        now,
        DbReservationStatus.HELD,
        DbReservationStatus.CONFIRMED);
  }

  List<InventoryReservationJpaEntity> findAllByBookingIdOrderByStayDateAscIdAsc(UUID bookingId);

  @Query(
      """
      SELECT ir FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId = :roomTypeId
        AND ir.stayDate >= :startDate
        AND ((ir.status = :heldStatus AND ir.expiresAt > :now)
             OR ir.status = :confirmedStatus)
      """)
  List<InventoryReservationJpaEntity> findActiveReservationsFrom(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("now") Instant now,
      @Param("heldStatus") DbReservationStatus heldStatus,
      @Param("confirmedStatus") DbReservationStatus confirmedStatus);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT ir FROM InventoryReservationJpaEntity ir
      WHERE ir.bookingId = :bookingId
      ORDER BY ir.stayDate ASC, ir.id ASC
      """)
  List<InventoryReservationJpaEntity> lockAllByBookingIdOrderByStayDateAscIdAsc(
      @Param("bookingId") UUID bookingId);

  // Theo lô cho tìm kiếm: cùng điều kiện với sumOccupancyByDateRange, gom theo loại phòng và ngày.
  @Query(
      """
      SELECT ir.roomTypeId AS roomTypeId, ir.stayDate AS stayDate,
             COALESCE(SUM(ir.quantity), 0) AS totalQuantity
      FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId IN :roomTypeIds
        AND ir.stayDate >= :startDate
        AND ir.stayDate < :endDate
        AND ((ir.status = :heldStatus AND ir.expiresAt > :now)
             OR ir.status = :confirmedStatus)
      GROUP BY ir.roomTypeId, ir.stayDate
      """)
  List<RoomTypeDailyOccupancy> sumOccupancyByRoomTypeAndDate(
      @Param("roomTypeIds") Collection<UUID> roomTypeIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("now") Instant now,
      @Param("heldStatus") DbReservationStatus heldStatus,
      @Param("confirmedStatus") DbReservationStatus confirmedStatus);

  default List<RoomTypeDailyOccupancy> sumOccupancyByRoomTypeAndDate(
      Collection<UUID> roomTypeIds, LocalDate startDate, LocalDate endDate, Instant now) {
    return sumOccupancyByRoomTypeAndDate(
        roomTypeIds,
        startDate,
        endDate,
        now,
        DbReservationStatus.HELD,
        DbReservationStatus.CONFIRMED);
  }

  @Query(
      """
      SELECT ir FROM InventoryReservationJpaEntity ir
      WHERE ir.roomTypeId IN :roomTypeIds
        AND ir.stayDate >= :startDate AND ir.stayDate < :endDate
        AND ((ir.status = :heldStatus AND ir.expiresAt > :now) OR ir.status = :confirmedStatus)
      """)
  List<InventoryReservationJpaEntity> findActiveReservationsForRoomTypes(
      @Param("roomTypeIds") Collection<UUID> roomTypeIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("now") Instant now,
      @Param("heldStatus") DbReservationStatus heldStatus,
      @Param("confirmedStatus") DbReservationStatus confirmedStatus);
}
