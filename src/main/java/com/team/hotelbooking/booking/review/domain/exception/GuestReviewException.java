package com.team.hotelbooking.booking.review.domain.exception;

public final class GuestReviewException extends RuntimeException {
  private final GuestReviewErrorCode code;

  public GuestReviewException(GuestReviewErrorCode code) {
    super(code.defaultMessage());
    this.code = code;
  }

  public GuestReviewException(GuestReviewErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public GuestReviewErrorCode code() {
    return code;
  }
}
