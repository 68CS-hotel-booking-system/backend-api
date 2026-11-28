package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.review.application.port.GuestReviewHotelPort;
import com.team.hotelbooking.hotel.HotelDirectory;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuestReviewHotelAdapter implements GuestReviewHotelPort {
  private final HotelDirectory hotels;

  public Optional<Hotel> findById(UUID hotelId) {
    return hotels.findById(hotelId).map(h -> new Hotel(h.ownerId(), h.name(), h.active()));
  }
}
