package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.domain.model.Booking;

public interface BookingInventoryPort {
  void hold(Booking booking);
}
