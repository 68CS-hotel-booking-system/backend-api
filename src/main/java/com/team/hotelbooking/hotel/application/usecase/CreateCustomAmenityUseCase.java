package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.CreateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.result.CustomAmenityResult;

public interface CreateCustomAmenityUseCase {
  CustomAmenityResult execute(CreateCustomAmenityCommand command);
}
