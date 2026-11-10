package com.team.hotelbooking.hotel.domain.exception;

import com.team.hotelbooking.hotel.domain.model.HotelPublishRequirement;
import java.util.List;

public final class HotelException extends RuntimeException {

  private final HotelErrorCode code;
  private final List<HotelPublishRequirement> unmetRequirements;

  public HotelException(HotelErrorCode code) {
    this(code, code.defaultMessage());
  }

  public HotelException(HotelErrorCode code, String message) {
    super(message);
    this.code = code;
    this.unmetRequirements = List.of();
  }

  // HT-03: liệt kê đủ mọi điều kiện còn thiếu để quản trị bổ sung một lần.
  public static HotelException notReady(List<HotelPublishRequirement> unmetRequirements) {
    return new HotelException(unmetRequirements);
  }

  private HotelException(List<HotelPublishRequirement> unmetRequirements) {
    super(HotelErrorCode.HOTEL_NOT_READY.defaultMessage());
    this.code = HotelErrorCode.HOTEL_NOT_READY;
    this.unmetRequirements = List.copyOf(unmetRequirements);
  }

  public List<HotelPublishRequirement> unmetRequirements() {
    return unmetRequirements;
  }

  public HotelErrorCode code() {
    return code;
  }

  public HotelErrorCode getErrorCode() {
    return code;
  }

  public static HotelException invalid(String message) {
    return new HotelException(HotelErrorCode.VALIDATION_FAILED, message);
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
