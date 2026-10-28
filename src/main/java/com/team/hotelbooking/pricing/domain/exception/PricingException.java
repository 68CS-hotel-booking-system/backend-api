package com.team.hotelbooking.pricing.domain.exception;

public final class PricingException extends RuntimeException {

  private final PricingErrorCode code;

  public PricingException(PricingErrorCode code) {
    this(code, code.defaultMessage());
  }

  public PricingException(PricingErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public PricingErrorCode code() {
    return code;
  }

  public static PricingException invalidDateRange(String message) {
    return new PricingException(PricingErrorCode.INVALID_DATE_RANGE, message);
  }

  public static PricingException maxRangeExceeded(String message) {
    return new PricingException(PricingErrorCode.MAX_RANGE_EXCEEDED, message);
  }

  public static PricingException hotelNotFound(String message) {
    return new PricingException(PricingErrorCode.HOTEL_NOT_FOUND, message);
  }

  public static PricingException roomTypeNotFound(String message) {
    return new PricingException(PricingErrorCode.ROOM_TYPE_NOT_FOUND, message);
  }
}
