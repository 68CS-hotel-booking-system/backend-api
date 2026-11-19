package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.CreateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;

public interface CreateRoomBlockUseCase {

  RoomBlockResult execute(CreateRoomBlockCommand command);
}
