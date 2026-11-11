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

  // HT21: field null được giữ nguyên. Ngừng sử dụng đặt mốc kết thúc phục vụ là now (ghi đè mốc
  // cũ); bật lại thì xóa mốc kết thúc. Giữ nguyên readinessUpdatedAt/By như hành vi trước đây.
  public Room update(
      String roomNumber, String floor, Readiness readiness, Boolean active, Instant now) {
    if (now == null) {
      throw HotelException.invalid("Thời điểm cập nhật phòng là bắt buộc.");
    }

    String newRoomNumber = this.roomNumber;
    if (roomNumber != null) {
      newRoomNumber = roomNumber.trim();
      if (newRoomNumber.isEmpty()) {
        throw HotelException.invalidRequest("Số phòng không được để trống.");
      }
    }

    Instant newOperationalUntil = this.operationalUntil;
    if (active != null) {
      newOperationalUntil = active ? null : now;
    }

    return new Room(
        id,
        hotelId,
        roomTypeId,
        newRoomNumber,
        floor != null ? floor : this.floor,
        readiness != null ? readiness : this.readiness,
        readinessUpdatedAt,
        readinessUpdatedBy,
        operationalFrom,
        newOperationalUntil,
        createdAt,
        now,
        version);
  }

  // Phòng còn phục vụ khi chưa có mốc kết thúc hoặc mốc kết thúc nằm sau thời điểm xét.
  public boolean activeAt(Instant instant) {
    return operationalUntil == null || operationalUntil.isAfter(instant);
  }
}
