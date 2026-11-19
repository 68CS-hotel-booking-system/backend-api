package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.UpdateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;

public interface UpdateRoomTypeUseCase {

  RoomTypeResult execute(UpdateRoomTypeCommand command);
}
