package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingHotelAdapter implements PricingHotelPort {

  private final HotelDirectory hotelDirectory;

  @Override
  public boolean isHotelActive(UUID hotelId) {
    return hotelDirectory.isHotelActive(hotelId);
  }

  @Override
  public boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId) {
    return hotelDirectory.isRoomTypeActiveInHotel(roomTypeId, hotelId);
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
