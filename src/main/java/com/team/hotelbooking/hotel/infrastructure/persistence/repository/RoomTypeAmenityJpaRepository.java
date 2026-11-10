package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeAmenityJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeAmenityJpaRepository
    extends JpaRepository<RoomTypeAmenityJpaEntity, RoomTypeAmenityJpaEntity.Key> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RoomTypeAmenityJpaEntity a where a.id.roomTypeId = :roomTypeId")
  int deleteByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);
}
