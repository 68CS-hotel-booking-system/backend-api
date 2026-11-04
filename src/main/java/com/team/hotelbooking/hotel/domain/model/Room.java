package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;

public record Room(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    Readiness readiness,
    Instant readinessUpdatedAt,
    UUID readinessUpdatedBy,
    Instant operationalFrom,
    Instant operationalUntil,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public enum Readiness {
    READY,
    DIRTY
  }

  public static Room create(
      UUID id,
      UUID hotelId,
      UUID roomTypeId,
      String roomNumber,
      String floor,
      Instant operationalFrom,
      Instant operationalUntil,
      UUID actorId,
      Instant now) {
    if (id == null || hotelId == null || roomTypeId == null || actorId == null || now == null) {
      throw HotelException.invalid(
          "Định danh phòng, khách sạn, loại phòng, người thực hiện và thời điểm tạo là bắt buộc.");
    }

    String normalizedRoomNumber = roomNumber == null ? "" : roomNumber.trim();
    if (normalizedRoomNumber.isBlank() || normalizedRoomNumber.length() > 20) {
      throw HotelException.invalid("Số phòng phải có từ 1 đến 20 ký tự.");
    }

    String normalizedFloor = floor == null ? null : floor.trim();
    if (normalizedFloor != null) {
      if (normalizedFloor.length() > 20) {
        throw HotelException.invalid("Tầng không được vượt quá 20 ký tự.");
      }
      if (normalizedFloor.isBlank()) {
        normalizedFloor = null;
      }
    }

    if (operationalFrom == null) {
      throw HotelException.invalid("Thời điểm bắt đầu phục vụ không được để trống.");
    }
    if (operationalUntil != null && !operationalUntil.isAfter(operationalFrom)) {
      throw HotelException.invalid(
          "Thời điểm kết thúc phục vụ phải sau thời điểm bắt đầu phục vụ.");
    }

    return new Room(
        id,
        hotelId,
        roomTypeId,
        normalizedRoomNumber,
        normalizedFloor,
        Readiness.DIRTY,
        now,
        actorId,
        operationalFrom,
        operationalUntil,
        now,
        now,
        0L);
  }
}
