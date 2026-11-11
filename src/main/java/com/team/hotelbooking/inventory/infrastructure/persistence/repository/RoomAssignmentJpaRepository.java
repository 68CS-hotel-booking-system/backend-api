package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomAssignmentJpaRepository extends JpaRepository<RoomAssignmentJpaEntity, UUID> {

  @Query(
      "SELECT ra FROM RoomAssignmentJpaEntity ra "
          + "WHERE ra.bookingId = :bookingId "
          + "AND ra.status IN ('ASSIGNED', 'CHECKED_IN') "
          + "ORDER BY ra.assignedAt ASC, ra.id ASC")
  List<RoomAssignmentJpaEntity> findActiveByBookingId(@Param("bookingId") UUID bookingId);

  @Query(
      "SELECT ra FROM RoomAssignmentJpaEntity ra "
          + "WHERE ra.bookingId = :bookingId "
          + "ORDER BY ra.assignedAt ASC, ra.id ASC")
  List<RoomAssignmentJpaEntity> findAllByBookingId(@Param("bookingId") UUID bookingId);

  @Query(
      "SELECT ra FROM RoomAssignmentJpaEntity ra "
          + "WHERE ra.roomId = :roomId "
          + "AND ra.status IN ('ASSIGNED', 'CHECKED_IN') "
          + "AND ra.startDate < :endDate "
          + "AND ra.endDate > :startDate")
  List<RoomAssignmentJpaEntity> findActiveByRoomIdOverlapping(
      @Param("roomId") UUID roomId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT ra FROM RoomAssignmentJpaEntity ra "
          + "WHERE ra.hotelId = :hotelId "
          + "AND ra.status IN ('ASSIGNED', 'CHECKED_IN') "
          + "AND ra.startDate < :endDate "
          + "AND ra.endDate > :startDate")
  List<RoomAssignmentJpaEntity> findActiveByHotelIdOverlapping(
      @Param("hotelId") UUID hotelId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT ra FROM RoomAssignmentJpaEntity ra "
          + "WHERE ra.roomTypeId IN :roomTypeIds "
          + "AND ra.status IN ('ASSIGNED', 'CHECKED_IN') "
          + "AND ra.startDate < :endDate "
          + "AND ra.endDate > :startDate")
  List<RoomAssignmentJpaEntity> findActiveByRoomTypeIdsOverlapping(
      @Param("roomTypeIds") Collection<UUID> roomTypeIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
