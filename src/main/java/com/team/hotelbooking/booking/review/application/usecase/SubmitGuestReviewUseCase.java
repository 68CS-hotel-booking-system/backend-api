package com.team.hotelbooking.booking.review.application.usecase;

import com.team.hotelbooking.booking.review.application.command.SubmitGuestReviewCommand;
import com.team.hotelbooking.booking.review.application.result.GuestReviewResult;

public interface SubmitGuestReviewUseCase {
  GuestReviewResult execute(SubmitGuestReviewCommand command);
}
