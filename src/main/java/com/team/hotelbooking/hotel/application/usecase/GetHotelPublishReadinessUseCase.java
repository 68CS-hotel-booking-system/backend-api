package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.GetHotelPublishReadinessQuery;
import com.team.hotelbooking.hotel.application.result.HotelPublishReadinessResult;

public interface GetHotelPublishReadinessUseCase {
  HotelPublishReadinessResult execute(GetHotelPublishReadinessQuery query);
}
