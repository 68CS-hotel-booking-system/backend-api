package com.team.hotelbooking.pricing.application.port;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface PricingHotelPort {

  boolean isHotelActive(UUID hotelId);

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);

  Optional<HotelPricingSnapshot> findActiveHotel(UUID hotelId);

  Optional<RoomTypePricingSnapshot> findActiveRoomType(UUID roomTypeId, UUID hotelId);

  record HotelPricingSnapshot(UUID id, LocalTime checkInTime, LocalTime checkOutTime) {}

  record RoomTypePricingSnapshot(
      UUID id,
      UUID hotelId,
      String name,
      int maxAdults,
      int maxChildren,
      int maxOccupancy,
      long basePrice) {}
}
