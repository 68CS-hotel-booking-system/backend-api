package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;

public interface MarkNoShowUseCase {

  BookingStayOperationResult execute(MarkNoShowCommand command);
}
