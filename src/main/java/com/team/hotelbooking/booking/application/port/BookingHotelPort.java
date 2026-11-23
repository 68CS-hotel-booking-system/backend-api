package com.team.hotelbooking.booking.application.port;

import java.util.Optional;
import java.util.UUID;

public interface BookingHotelPort {

  Optional<HotelSnapshot> findById(UUID hotelId);

  boolean canManageVouchers(UUID hotelId, UUID actorId, boolean admin);

  record HotelSnapshot(UUID id, UUID ownerId, String name, boolean active) {}
}
