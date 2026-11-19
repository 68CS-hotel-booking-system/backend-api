package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.UpdateHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface UpdateHotelUseCase {

  ManagementHotelResult execute(UpdateHotelCommand command);
}
