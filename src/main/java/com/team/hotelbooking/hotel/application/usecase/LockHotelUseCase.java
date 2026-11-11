package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.LockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface LockHotelUseCase {

  ManagementHotelResult execute(LockHotelCommand command);
}
