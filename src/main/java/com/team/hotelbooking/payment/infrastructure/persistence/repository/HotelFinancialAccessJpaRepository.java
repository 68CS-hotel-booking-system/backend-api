package com.team.hotelbooking.payment.infrastructure.persistence.repository;

import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelFinancialAccessJpaRepository
    extends JpaRepository<HotelFinancialAccessJpaEntity, UUID> {

  Optional<HotelFinancialAccessJpaEntity> findByHotelIdAndUserId(UUID hotelId, UUID userId);

  boolean existsByHotelIdAndUserIdAndGrantedTrue(UUID hotelId, UUID userId);
}
