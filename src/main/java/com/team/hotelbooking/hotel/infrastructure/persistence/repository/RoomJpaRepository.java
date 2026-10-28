package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {
  long countByRoomTypeIdAndActiveTrue(UUID roomTypeId);

  List<RoomJpaEntity> findAllByRoomTypeIdAndActiveTrue(UUID roomTypeId);
}
