package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelJpaRepository extends JpaRepository<HotelJpaEntity, UUID> {
  Optional<HotelJpaEntity> findByIdAndActiveTrue(UUID id);
}
