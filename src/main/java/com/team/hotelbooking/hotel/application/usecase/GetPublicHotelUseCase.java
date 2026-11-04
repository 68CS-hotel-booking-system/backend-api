package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import java.util.UUID;

public interface GetPublicHotelUseCase {

  GetPublicHotelResult execute(UUID hotelId);
}
