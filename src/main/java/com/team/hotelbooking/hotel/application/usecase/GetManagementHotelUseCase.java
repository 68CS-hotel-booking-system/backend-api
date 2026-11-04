package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface GetManagementHotelUseCase {

  ManagementHotelResult execute(GetManagementHotelQuery query);
}
