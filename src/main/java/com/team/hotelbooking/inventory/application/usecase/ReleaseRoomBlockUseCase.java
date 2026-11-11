package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.ReleaseRoomBlockCommand;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;

public interface ReleaseRoomBlockUseCase {

  RoomBlockResult execute(ReleaseRoomBlockCommand command);
}
