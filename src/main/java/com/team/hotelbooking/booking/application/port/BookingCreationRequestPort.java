package com.team.hotelbooking.booking.application.port;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import java.util.UUID;

public interface BookingCreationRequestPort {
  UUID claim(CreateBookingCommand command, UUID proposedBookingId);
}
