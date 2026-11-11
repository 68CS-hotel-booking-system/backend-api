package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.SubmitHotelReviewCommand;
import com.team.hotelbooking.hotel.application.result.HotelReviewResult;

public interface SubmitHotelReviewUseCase {
  HotelReviewResult execute(SubmitHotelReviewCommand command);
}
