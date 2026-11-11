package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.DetachCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.result.CustomAmenityResult;

public interface DetachCustomAmenityUseCase {
  CustomAmenityResult execute(DetachCustomAmenityCommand command);
}
