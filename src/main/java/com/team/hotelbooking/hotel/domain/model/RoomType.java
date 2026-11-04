package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.time.Instant;
import java.util.UUID;

public record RoomType(
    UUID id,
    UUID hotelId,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static RoomType create(
      UUID id,
      UUID hotelId,
      String name,
      String description,
      short maxAdults,
      short maxChildren,
      Instant now) {
    if (id == null || hotelId == null || now == null) {
      throw HotelException.invalid("Định danh loại phòng, khách sạn và thời điểm tạo là bắt buộc.");
    }

    String normalizedName = name == null ? "" : name.trim();
    if (normalizedName.isBlank() || normalizedName.length() > 100) {
      throw HotelException.invalid("Tên loại phòng phải có từ 1 đến 100 ký tự.");
    }

    if (maxAdults < 1) {
      throw HotelException.invalid("Số người lớn tối đa phải từ 1.");
    }
    if (maxChildren < 0) {
      throw HotelException.invalid("Số trẻ em tối đa không được âm.");
    }

    return new RoomType(
        id, hotelId, normalizedName, description, maxAdults, maxChildren, true, now, now, 0L);
  }
}
