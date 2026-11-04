package com.team.hotelbooking.pricing.application.usecase;

import com.team.hotelbooking.pricing.application.command.SetRoomRatesCommand;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;

public interface SetRoomRatesUseCase {

  RoomTypeRatesResult execute(SetRoomRatesCommand command);
}
