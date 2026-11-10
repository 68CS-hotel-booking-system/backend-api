package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import java.util.Optional;
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

  @Override
  public Optional<HotelPricingSnapshot> findActiveHotel(UUID hotelId) {
    return hotelJpaRepository
        .findByIdAndActiveTrue(hotelId)
        .map(h -> new HotelPricingSnapshot(h.getId(), h.getCheckInTime(), h.getCheckOutTime()));
  }

  @Override
  public Optional<RoomTypePricingSnapshot> findActiveRoomType(UUID roomTypeId, UUID hotelId) {
    return roomTypeJpaRepository
        .findByIdAndHotelIdAndActiveTrue(roomTypeId, hotelId)
        .map(
            rt ->
                new RoomTypePricingSnapshot(
                    rt.getId(),
                    rt.getHotelId(),
                    rt.getName(),
                    rt.getMaxAdults(),
                    rt.getMaxChildren(),
                    rt.getMaxOccupancy(),
                    rt.getBasePrice()));
  }
}
