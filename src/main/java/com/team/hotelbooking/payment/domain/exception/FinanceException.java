package com.team.hotelbooking.payment.domain.exception;

public final class FinanceException extends RuntimeException {

  private final FinanceErrorCode code;

  public FinanceException(FinanceErrorCode code) {
    this(code, code.defaultMessage());
  }

  public FinanceException(FinanceErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public FinanceErrorCode code() {
    return code;
  }

  public static FinanceException contractCodeExists(String message) {
    return new FinanceException(FinanceErrorCode.CONTRACT_CODE_ALREADY_EXISTS, message);
  }

  public static FinanceException invalidPeriod(String message) {
    return new FinanceException(FinanceErrorCode.INVALID_CONTRACT_PERIOD, message);
  }

  public static FinanceException invalidRate(String message) {
    return new FinanceException(FinanceErrorCode.INVALID_COMMISSION_RATE, message);
  }

  public static FinanceException activeContractOverlap(String message) {
    return new FinanceException(FinanceErrorCode.ACTIVE_CONTRACT_PERIOD_OVERLAP, message);
  }

  public static FinanceException hotelNotFound(String message) {
    return new FinanceException(FinanceErrorCode.HOTEL_NOT_FOUND, message);
  }

  public static FinanceException userNotFound(String message) {
    return new FinanceException(FinanceErrorCode.USER_NOT_FOUND, message);
  }

  public static FinanceException accessNotFound(String message) {
    return new FinanceException(FinanceErrorCode.FINANCIAL_ACCESS_NOT_FOUND, message);
  }

  public static FinanceException concurrentModification(String message) {
    return new FinanceException(FinanceErrorCode.CONCURRENT_MODIFICATION_DETECTED, message);
  }

  public static FinanceException accessDenied(String message) {
    return new FinanceException(FinanceErrorCode.ACCESS_DENIED, message);
  }
}
