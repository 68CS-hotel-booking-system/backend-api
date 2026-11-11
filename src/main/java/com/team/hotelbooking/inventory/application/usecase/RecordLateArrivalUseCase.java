package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.RecordLateArrivalCommand;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;

public interface RecordLateArrivalUseCase {

  BookingStayOperationResult execute(RecordLateArrivalCommand command);
}
