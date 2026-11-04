package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentHotelAdapter implements PaymentHotelPort {

  private final HotelJpaRepository hotelJpaRepository;

  @Override
  public boolean existsById(UUID hotelId) {
    return hotelJpaRepository.existsById(hotelId);
  }

  @Override
  public Optional<UUID> findOwnerUserId(UUID hotelId) {
    return hotelJpaRepository.findById(hotelId).map(HotelJpaEntity::getOwnerUserId);
  }
}
