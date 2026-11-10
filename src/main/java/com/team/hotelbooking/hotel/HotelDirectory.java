package com.team.hotelbooking.hotel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelDirectory {

  // Phục vụ module payment
  boolean existsById(UUID hotelId);

  Optional<UUID> findOwnerUserId(UUID hotelId);

  // Phục vụ module pricing
  boolean isHotelActive(UUID hotelId);

  boolean isRoomTypeActiveInHotel(UUID roomTypeId, UUID hotelId);

  // Phục vụ module inventory
  Optional<HotelSummaryView> findHotelSummary(UUID hotelId);

  List<RoomTypeSummaryView> findActiveRoomTypes(UUID hotelId);

  long countActiveRoomsByRoomTypeId(UUID roomTypeId);

  // DTO Records dùng chung
  record HotelSummaryView(UUID id, boolean active, boolean verified) {}

  record RoomTypeSummaryView(
      UUID id, String name, int maxOccupancy, int maxAdults, int maxChildren, long basePrice) {}
}
