package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.RejectHotelReviewCommand;
import com.team.hotelbooking.hotel.application.result.HotelReviewResult;

public interface RejectHotelReviewUseCase {
  HotelReviewResult execute(RejectHotelReviewCommand command);
}
