package com.team.hotelbooking.inventory.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelCatalogPort {

  record HotelSummary(UUID id, boolean isActive, boolean isSuspended) {}

  record RoomTypeSummary(
      UUID id, String name, int maxOccupancy, int maxAdults, int maxChildren, Long basePrice) {}

  Optional<HotelSummary> findHotelSummary(UUID hotelId);

  List<RoomTypeSummary> findActiveRoomTypes(UUID hotelId);
}
