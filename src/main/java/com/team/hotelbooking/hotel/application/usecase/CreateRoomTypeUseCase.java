package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.CreateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.CreateRoomTypeResult;

public interface CreateRoomTypeUseCase {

  CreateRoomTypeResult execute(CreateRoomTypeCommand command);
}
