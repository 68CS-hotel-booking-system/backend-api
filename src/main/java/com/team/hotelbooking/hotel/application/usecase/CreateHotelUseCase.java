package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.CreateHotelCommand;
import com.team.hotelbooking.hotel.application.result.CreateHotelResult;

public interface CreateHotelUseCase {

  CreateHotelResult execute(CreateHotelCommand command);
}
