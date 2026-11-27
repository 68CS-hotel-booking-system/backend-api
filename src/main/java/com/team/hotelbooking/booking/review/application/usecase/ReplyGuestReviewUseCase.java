package com.team.hotelbooking.booking.review.application.usecase;

import com.team.hotelbooking.booking.review.application.command.ReplyGuestReviewCommand;
import com.team.hotelbooking.booking.review.application.result.GuestReviewResult;

public interface ReplyGuestReviewUseCase {
  GuestReviewResult execute(ReplyGuestReviewCommand command);
}
