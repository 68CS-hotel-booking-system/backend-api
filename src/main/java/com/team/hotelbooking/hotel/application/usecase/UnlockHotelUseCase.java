package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.UnlockHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface UnlockHotelUseCase {

  ManagementHotelResult execute(UnlockHotelCommand command);
}
