package com.team.hotelbooking.booking.review.application.port;

import java.util.Optional;
import java.util.UUID;

public interface GuestReviewHotelPort {
  Optional<Hotel> findById(UUID hotelId);

  record Hotel(UUID ownerId, String name, boolean active) {}
}
