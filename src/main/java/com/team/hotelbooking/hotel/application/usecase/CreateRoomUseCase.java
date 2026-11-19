package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.CreateRoomCommand;
import com.team.hotelbooking.hotel.application.result.CreateRoomResult;

public interface CreateRoomUseCase {

  CreateRoomResult execute(CreateRoomCommand command);
}
