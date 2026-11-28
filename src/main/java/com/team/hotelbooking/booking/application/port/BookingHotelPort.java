package com.team.hotelbooking.booking.application.port;

import java.util.Optional;
import java.util.UUID;

public interface BookingHotelPort {

  Optional<HotelSnapshot> findById(UUID hotelId);

  default boolean hasOperationalAccess(UUID hotelId, UUID actorId) {
    return false;
  }

  record HotelSnapshot(UUID id, UUID ownerId, String name, boolean active) {}
}
