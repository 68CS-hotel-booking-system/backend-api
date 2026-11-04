package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;

public interface CheckAvailabilityUseCase {
  CheckAvailabilityResult execute(CheckAvailabilityQuery query);
}
