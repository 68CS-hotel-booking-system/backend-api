package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.domain.model.Booking;
import java.util.UUID;

public interface BookingInventoryPort {
  void hold(Booking booking);

  void release(UUID bookingId, boolean expired);
}
