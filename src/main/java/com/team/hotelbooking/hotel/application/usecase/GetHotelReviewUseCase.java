package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.GetHotelReviewQuery;
import com.team.hotelbooking.hotel.application.result.HotelReviewResult;

public interface GetHotelReviewUseCase {
  HotelReviewResult execute(GetHotelReviewQuery query);
}
