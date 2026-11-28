package com.team.hotelbooking.booking.review.application.usecase;

import com.team.hotelbooking.booking.review.application.command.GetBookingGuestReviewQuery;
import com.team.hotelbooking.booking.review.application.result.BookingGuestReviewResult;

public interface GetBookingGuestReviewUseCase {
  BookingGuestReviewResult execute(GetBookingGuestReviewQuery query);
}
