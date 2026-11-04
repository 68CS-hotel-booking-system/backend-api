package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

  Optional<RoomJpaEntity> findByIdAndHotelId(UUID id, UUID hotelId);

  boolean existsByHotelIdAndRoomNumberAndIdNot(UUID hotelId, String roomNumber, UUID id);

  @Query(
      """
      SELECT COUNT(r) FROM RoomJpaEntity r
      WHERE r.roomTypeId = :roomTypeId
        AND (r.operationalUntil IS NULL OR r.operationalUntil > CURRENT_TIMESTAMP)
      """)
  long countByRoomTypeIdAndActiveTrue(@Param("roomTypeId") UUID roomTypeId);

  @Query(
      """
      SELECT r FROM RoomJpaEntity r
      WHERE r.roomTypeId = :roomTypeId
        AND (r.operationalUntil IS NULL OR r.operationalUntil > CURRENT_TIMESTAMP)
      """)
  List<RoomJpaEntity> findAllByRoomTypeIdAndActiveTrue(@Param("roomTypeId") UUID roomTypeId);
}
