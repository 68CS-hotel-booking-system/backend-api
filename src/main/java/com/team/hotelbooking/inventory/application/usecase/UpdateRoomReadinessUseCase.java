package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.UpdateRoomReadinessCommand;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;

public interface UpdateRoomReadinessUseCase {

  RoomReadinessResult execute(UpdateRoomReadinessCommand command);
}
