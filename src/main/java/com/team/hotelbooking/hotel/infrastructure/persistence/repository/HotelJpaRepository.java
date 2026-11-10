package com.team.hotelbooking.hotel.infrastructure.persistence.repository;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HotelJpaRepository
    extends JpaRepository<HotelJpaEntity, UUID>, JpaSpecificationExecutor<HotelJpaEntity> {

  Optional<HotelJpaEntity> findByIdAndOwnerId(UUID hotelId, UUID ownerId);

  Optional<HotelJpaEntity> findByIdAndStatus(UUID hotelId, HotelJpaEntity.DbStatus status);

  Page<HotelJpaEntity> findByOwnerId(UUID ownerId, Pageable pageable);

  default Optional<HotelJpaEntity> findByIdAndActiveTrue(UUID id) {
    return findByIdAndStatus(id, HotelJpaEntity.DbStatus.ACTIVE);
  }
}
