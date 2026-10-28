package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeJpaEntity, UUID> {
  List<RoomTypeJpaEntity> findAllByHotelIdAndActiveTrue(UUID hotelId);

  Optional<RoomTypeJpaEntity> findByIdAndHotelIdAndActiveTrue(UUID id, UUID hotelId);
}
