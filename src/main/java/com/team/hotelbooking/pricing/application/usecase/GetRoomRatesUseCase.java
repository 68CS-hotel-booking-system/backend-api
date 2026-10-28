package com.team.hotelbooking.pricing.application.usecase;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;

public interface GetRoomRatesUseCase {
  RoomTypeRates execute(GetRoomRatesQuery query);
}
