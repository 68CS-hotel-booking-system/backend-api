package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomBlockJpaRepository extends JpaRepository<RoomBlockJpaEntity, UUID> {

  @Query(
      """
      SELECT COUNT(rb) FROM RoomBlockJpaEntity rb
      WHERE rb.roomTypeId = :roomTypeId
        AND rb.status = :status
        AND rb.startDate <= :date
        AND rb.endDate >= :date
      """)
  long countActiveBlocksOnDate(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("date") LocalDate date,
      @Param("status") RoomBlockJpaEntity.DbRoomBlockStatus status);

  default long countActiveBlocksOnDate(UUID roomTypeId, LocalDate date) {
    return countActiveBlocksOnDate(roomTypeId, date, RoomBlockJpaEntity.DbRoomBlockStatus.ACTIVE);
  }

  @Query(
      """
      SELECT rb FROM RoomBlockJpaEntity rb
      WHERE rb.roomTypeId = :roomTypeId
        AND rb.status = :status
        AND rb.startDate < :endDate
        AND rb.endDate >= :startDate
      """)
  List<RoomBlockJpaEntity> findActiveBlocksInRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("status") RoomBlockJpaEntity.DbRoomBlockStatus status);

  default List<RoomBlockJpaEntity> findActiveBlocksInRange(
      UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    return findActiveBlocksInRange(
        roomTypeId, startDate, endDate, RoomBlockJpaEntity.DbRoomBlockStatus.ACTIVE);
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT rb FROM RoomBlockJpaEntity rb
      WHERE rb.id = :blockId
        AND rb.hotelId = :hotelId
      """)
  Optional<RoomBlockJpaEntity> lockByIdAndHotelId(
      @Param("blockId") UUID blockId, @Param("hotelId") UUID hotelId);

  List<RoomBlockJpaEntity> findByHotelIdOrderByStartDateAscEndDateAscIdAsc(UUID hotelId);

  // Theo lô cho tìm kiếm: cùng điều kiện với findActiveBlocksInRange nhưng cho nhiều loại phòng.
  @Query(
      """
      SELECT rb FROM RoomBlockJpaEntity rb
      WHERE rb.roomTypeId IN :roomTypeIds
        AND rb.status = :status
        AND rb.startDate < :endDate
        AND rb.endDate >= :startDate
      """)
  List<RoomBlockJpaEntity> findActiveBlocksInRangeForRoomTypes(
      @Param("roomTypeIds") Collection<UUID> roomTypeIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("status") RoomBlockJpaEntity.DbRoomBlockStatus status);

  default List<RoomBlockJpaEntity> findActiveBlocksInRangeForRoomTypes(
      Collection<UUID> roomTypeIds, LocalDate startDate, LocalDate endDate) {
    return findActiveBlocksInRangeForRoomTypes(
        roomTypeIds, startDate, endDate, RoomBlockJpaEntity.DbRoomBlockStatus.ACTIVE);
  }
}
