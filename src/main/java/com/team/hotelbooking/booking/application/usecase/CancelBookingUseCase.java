package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.CancelBookingCommand;
import com.team.hotelbooking.booking.application.result.CancelBookingResult;

public interface CancelBookingUseCase {
  CancelBookingResult execute(CancelBookingCommand command);
}
