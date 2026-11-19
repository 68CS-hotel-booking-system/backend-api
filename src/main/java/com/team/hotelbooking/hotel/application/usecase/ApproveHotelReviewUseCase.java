package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ApproveHotelReviewCommand;
import com.team.hotelbooking.hotel.application.result.HotelReviewResult;

public interface ApproveHotelReviewUseCase {
  HotelReviewResult execute(ApproveHotelReviewCommand command);
}
