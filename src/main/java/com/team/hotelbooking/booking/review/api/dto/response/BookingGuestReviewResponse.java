package com.team.hotelbooking.booking.review.api.dto.response;

import com.team.hotelbooking.booking.review.application.result.BookingGuestReviewResult;

public record BookingGuestReviewResponse(
    boolean canReview, String message, GuestReviewResponse review) {
  public static BookingGuestReviewResponse from(BookingGuestReviewResult r) {
    return new BookingGuestReviewResponse(
        r.canReview(),
        r.message(),
        r.review() == null ? null : GuestReviewResponse.from(r.review()));
  }
}
