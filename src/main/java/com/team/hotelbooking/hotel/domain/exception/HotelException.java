package com.team.hotelbooking.hotel.domain.exception;

public final class HotelException extends RuntimeException {

  private final HotelErrorCode code;

  public HotelException(HotelErrorCode code) {
    this(code, code.defaultMessage());
  }

  public HotelException(HotelErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public HotelErrorCode code() {
    return code;
  }

  public static HotelException invalid(String message) {
    return new HotelException(HotelErrorCode.VALIDATION_FAILED, message);
  }
}
