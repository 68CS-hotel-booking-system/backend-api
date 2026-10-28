package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;

public interface CheckAvailabilityUseCase {
  HotelAvailability execute(CheckAvailabilityQuery query);
}
