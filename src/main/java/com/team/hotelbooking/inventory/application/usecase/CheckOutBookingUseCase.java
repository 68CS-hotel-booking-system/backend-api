package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.CheckOutBookingCommand;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;

public interface CheckOutBookingUseCase {

  BookingStayOperationResult execute(CheckOutBookingCommand command);
}
