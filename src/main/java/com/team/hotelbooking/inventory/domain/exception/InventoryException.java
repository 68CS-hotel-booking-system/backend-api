package com.team.hotelbooking.inventory.domain.exception;

public final class InventoryException extends RuntimeException {

  private final InventoryErrorCode code;

  public InventoryException(InventoryErrorCode code) {
    this(code, code.defaultMessage());
  }

  public InventoryException(InventoryErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public InventoryErrorCode code() {
    return code;
  }

  public static InventoryException invalidDateRange(String message) {
    return new InventoryException(InventoryErrorCode.INVALID_DATE_RANGE, message);
  }

  public static InventoryException maxStayExceeded(String message) {
    return new InventoryException(InventoryErrorCode.MAX_STAY_EXCEEDED, message);
  }

  public static InventoryException hotelNotFound(String message) {
    return new InventoryException(InventoryErrorCode.HOTEL_NOT_FOUND, message);
  }
}
