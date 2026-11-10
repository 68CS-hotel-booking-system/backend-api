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
    return hotelDirectory
        .findById(hotelId)
        .filter(HotelDirectory.HotelInfo::active)
        .map(h -> new HotelPricingSnapshot(h.id(), h.checkInTime(), h.checkOutTime()));
  }

  @Override
  public Optional<RoomTypePricingSnapshot> findActiveRoomType(UUID roomTypeId, UUID hotelId) {
    return hotelDirectory
        .findActiveRoomType(roomTypeId, hotelId)
        .map(
            rt ->
                new RoomTypePricingSnapshot(
                    rt.id(),
                    hotelId,
                    rt.name(),
                    rt.maxAdults(),
                    rt.maxChildren(),
                    rt.maxOccupancy(),
                    rt.basePrice()));
  }
}
