package com.team.hotelbooking.booking.domain.exception;

public final class BookingException extends RuntimeException {

  private final BookingErrorCode code;

  public BookingException(BookingErrorCode code) {
    this(code, code.defaultMessage());
  }

  public BookingException(BookingErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public BookingErrorCode code() {
    return code;
  }
}
