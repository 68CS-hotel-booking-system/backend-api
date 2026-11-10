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

  public static PricingException invalidAmount(String message) {
    return new PricingException(PricingErrorCode.INVALID_AMOUNT, message);
  }

  public static PricingException maxStayExceeded(String message) {
    return new PricingException(PricingErrorCode.MAX_STAY_EXCEEDED, message);
  }

  public static PricingException invalidQuoteRequest(String message) {
    return new PricingException(PricingErrorCode.INVALID_QUOTE_REQUEST, message);
  }

  public static PricingException invalidOccupancy(String message) {
    return new PricingException(PricingErrorCode.INVALID_OCCUPANCY, message);
  }

  public static PricingException rateNotAvailable(String message) {
    return new PricingException(PricingErrorCode.RATE_NOT_AVAILABLE, message);
  }
}
