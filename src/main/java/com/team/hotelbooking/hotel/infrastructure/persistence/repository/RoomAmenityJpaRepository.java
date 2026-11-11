package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomAmenityJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAmenityJpaRepository
    extends JpaRepository<RoomAmenityJpaEntity, RoomAmenityJpaEntity.Key> {
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RoomAmenityJpaEntity a where a.id.roomId = :roomId")
  int deleteByRoomId(@Param("roomId") UUID roomId);
}
