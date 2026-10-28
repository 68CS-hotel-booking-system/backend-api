package com.team.hotelbooking.inventory.infrastructure.persistence.repository;

import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomBlockJpaRepository extends JpaRepository<RoomBlockJpaEntity, UUID> {

  @Query(
      """
      SELECT COUNT(rb) FROM RoomBlockJpaEntity rb
      WHERE rb.roomTypeId = :roomTypeId
        AND rb.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity.DbRoomBlockStatus.ACTIVE
        AND rb.startDate <= :date
        AND rb.endDate >= :date
      """)
  long countActiveBlocksOnDate(@Param("roomTypeId") UUID roomTypeId, @Param("date") LocalDate date);

  @Query(
      """
      SELECT rb FROM RoomBlockJpaEntity rb
      WHERE rb.roomTypeId = :roomTypeId
        AND rb.status = com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity.DbRoomBlockStatus.ACTIVE
        AND rb.startDate < :endDate
        AND rb.endDate >= :startDate
      """)
  List<RoomBlockJpaEntity> findActiveBlocksInRange(
      @Param("roomTypeId") UUID roomTypeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
