package com.team.hotelbooking.hotel.domain.exception;

import lombok.Getter;

@Getter
public class HotelException extends RuntimeException {

  private final HotelErrorCode errorCode;

  public HotelException(HotelErrorCode errorCode) {
    super(errorCode.defaultMessage());
    this.errorCode = errorCode;
  }

  public HotelException(HotelErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
  }

  public static HotelException hotelNotFound(String message) {
    return new HotelException(HotelErrorCode.HOTEL_NOT_FOUND, message);
  }

  public static HotelException roomTypeNotFound(String message) {
    return new HotelException(HotelErrorCode.ROOM_TYPE_NOT_FOUND, message);
  }

  public static HotelException roomNotFound(String message) {
    return new HotelException(HotelErrorCode.ROOM_NOT_FOUND, message);
  }

  public static HotelException duplicateRoomNumber(String message) {
    return new HotelException(HotelErrorCode.DUPLICATE_ROOM_NUMBER, message);
  }

  public static HotelException invalidOccupancy(String message) {
    return new HotelException(HotelErrorCode.INVALID_OCCUPANCY, message);
  }

  public static HotelException invalidPrice(String message) {
    return new HotelException(HotelErrorCode.INVALID_PRICE, message);
  }

  public static HotelException invalidRequest(String message) {
    return new HotelException(HotelErrorCode.INVALID_REQUEST, message);
  }
}
