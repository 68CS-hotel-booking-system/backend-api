package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.hotel.HotelDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingHotelAdapter implements BookingHotelPort {

  private final HotelDirectory hotelDirectory;

  public BookingHotelAdapter(HotelDirectory hotelDirectory) {
    this.hotelDirectory = hotelDirectory;
  }

  @Override
  public Optional<HotelSnapshot> findById(UUID hotelId) {
    return hotelDirectory
        .findById(hotelId)
        .map(h -> new HotelSnapshot(h.id(), h.ownerId(), h.name(), h.active()));
  }
}
