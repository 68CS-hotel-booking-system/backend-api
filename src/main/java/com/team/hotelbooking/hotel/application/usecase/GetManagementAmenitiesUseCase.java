package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.GetManagementAmenitiesQuery;
import com.team.hotelbooking.hotel.application.result.ManagementAmenitiesResult;

public interface GetManagementAmenitiesUseCase {
  ManagementAmenitiesResult execute(GetManagementAmenitiesQuery command);
}
