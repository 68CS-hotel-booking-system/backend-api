package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;

public interface CreateBookingUseCase {

  BookingDetailResult execute(CreateBookingCommand command);
}
