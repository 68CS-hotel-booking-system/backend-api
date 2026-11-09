package com.team.hotelbooking.pricing.infrastructure.integration;

import com.team.hotelbooking.hotel.HotelDirectory; // Import duy nhất từ hotel
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
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
}
