package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.CancelHotelBookingCommand;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;

public interface CancelHotelBookingUseCase {
  CancelBookingResult execute(CancelHotelBookingCommand command);
}
