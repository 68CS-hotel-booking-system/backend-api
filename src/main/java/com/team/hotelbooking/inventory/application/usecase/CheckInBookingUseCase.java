package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.CheckInBookingCommand;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;

public interface CheckInBookingUseCase {

  BookingStayOperationResult execute(CheckInBookingCommand command);
}
