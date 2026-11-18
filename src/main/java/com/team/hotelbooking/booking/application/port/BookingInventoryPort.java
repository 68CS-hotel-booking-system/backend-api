package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.domain.model.Booking;
import java.time.Instant;
import java.util.UUID;

public interface BookingInventoryPort {
  void hold(Booking booking);

  boolean confirmIfHeld(Booking booking);

  void release(UUID bookingId, boolean expired);

  void releaseConfirmed(UUID bookingId, UUID actorId, Instant now);
}
