package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;

public interface AssignRoomsUseCase {

  BookingRoomAssignmentsResult execute(AssignRoomsCommand command);
}
