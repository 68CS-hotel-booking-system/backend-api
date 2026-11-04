package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingHotelAdapter implements PricingHotelPort {

  private final HotelJpaRepository hotelJpaRepository;
  private final RoomTypeJpaRepository roomTypeJpaRepository;

  @Override
  public boolean isHotelActive(UUID hotelId) {
    return hotelJpaRepository.findByIdAndActiveTrue(hotelId).isPresent();
  }

  @Override
  public boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository.findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId).isPresent();
  }
}
