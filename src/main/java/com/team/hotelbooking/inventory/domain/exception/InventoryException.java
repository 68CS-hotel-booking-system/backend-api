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

  public static InventoryException invalidSearchCriteria(String message) {
    return new InventoryException(InventoryErrorCode.INVALID_SEARCH_CRITERIA, message);
  }

  public static InventoryException roomTypeInactive(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_TYPE_INACTIVE, message);
  }

  public static InventoryException rateNotSet(String message) {
    return new InventoryException(InventoryErrorCode.RATE_NOT_SET, message);
  }

  public static InventoryException insufficientInventory(String message) {
    return new InventoryException(InventoryErrorCode.INSUFFICIENT_INVENTORY, message);
  }

  public static InventoryException holdExpired(String message) {
    return new InventoryException(InventoryErrorCode.HOLD_EXPIRED, message);
  }

  public static InventoryException roomNotFound(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_NOT_FOUND, message);
  }

  public static InventoryException roomBlockNotFound(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_BLOCK_NOT_FOUND, message);
  }

  public static InventoryException roomBlockOverlap(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_BLOCK_OVERLAP, message);
  }

  public static InventoryException roomBlockConflictsReservation(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_BLOCK_CONFLICTS_RESERVATION, message);
  }

  public static InventoryException roomBlockAlreadyReleased(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_BLOCK_ALREADY_RELEASED, message);
  }

  public static InventoryException bookingNotFound(String message) {
    return new InventoryException(InventoryErrorCode.BOOKING_NOT_FOUND, message);
  }

  public static InventoryException invalidStayOperation(String message) {
    return new InventoryException(InventoryErrorCode.INVALID_STAY_OPERATION, message);
  }

  public static InventoryException roomAssignmentConflict(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_ASSIGNMENT_CONFLICT, message);
  }

  public static InventoryException roomNotReady(String message) {
    return new InventoryException(InventoryErrorCode.ROOM_NOT_READY, message);
  }
}
