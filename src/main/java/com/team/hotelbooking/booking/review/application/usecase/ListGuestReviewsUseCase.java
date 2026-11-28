package com.team.hotelbooking.booking.review.application.usecase;

import com.team.hotelbooking.booking.review.application.command.ListGuestReviewsQuery;
import com.team.hotelbooking.booking.review.application.result.ListGuestReviewsResult;

public interface ListGuestReviewsUseCase {
  ListGuestReviewsResult execute(ListGuestReviewsQuery query);
}
