package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ReplaceRoomAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ManagementAmenitiesResult;

public interface ReplaceRoomAmenitiesUseCase {
  ManagementAmenitiesResult execute(ReplaceRoomAmenitiesCommand command);
}
