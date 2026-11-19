package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.SuspendHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface SuspendHotelUseCase {

  ManagementHotelResult execute(SuspendHotelCommand command);
}
