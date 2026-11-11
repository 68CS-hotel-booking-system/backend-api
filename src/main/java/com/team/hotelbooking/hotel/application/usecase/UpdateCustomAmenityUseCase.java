package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.UpdateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.result.CustomAmenityResult;

public interface UpdateCustomAmenityUseCase {
  CustomAmenityResult execute(UpdateCustomAmenityCommand command);
}
