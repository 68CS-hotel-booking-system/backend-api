package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.UpdateRoomCommand;
import com.team.hotelbooking.hotel.application.result.RoomResult;

public interface UpdateRoomUseCase {

  RoomResult execute(UpdateRoomCommand command);
}
