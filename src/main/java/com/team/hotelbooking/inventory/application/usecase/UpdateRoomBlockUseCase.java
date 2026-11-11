package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.UpdateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;

public interface UpdateRoomBlockUseCase {

  RoomBlockResult execute(UpdateRoomBlockCommand command);
}
