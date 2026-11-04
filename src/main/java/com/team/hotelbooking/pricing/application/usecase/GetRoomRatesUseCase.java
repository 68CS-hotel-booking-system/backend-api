package com.team.hotelbooking.pricing.application.usecase;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;

public interface GetRoomRatesUseCase {
  RoomTypeRatesResult execute(GetRoomRatesQuery query);
}
