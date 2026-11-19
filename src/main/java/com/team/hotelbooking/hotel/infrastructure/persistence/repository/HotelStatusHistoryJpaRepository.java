package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelStatusHistoryJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelStatusHistoryJpaRepository
    extends JpaRepository<HotelStatusHistoryJpaEntity, UUID> {

  Optional<HotelStatusHistoryJpaEntity> findFirstByHotelIdAndToStatusOrderByChangedAtDesc(
      UUID hotelId, HotelJpaEntity.DbStatus toStatus);
}
