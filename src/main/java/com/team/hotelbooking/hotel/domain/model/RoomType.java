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
    short maxOccupancy,
    long basePrice,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  // Trùng DEFAULT của cột max_occupancy/base_price trong migration V20261104190000000.
  static final short DEFAULT_MAX_OCCUPANCY = 2;
  static final long DEFAULT_BASE_PRICE = 0L;

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
        id,
        hotelId,
        normalizedName,
        description,
        maxAdults,
        maxChildren,
        (short) Math.max(DEFAULT_MAX_OCCUPANCY, Math.max(maxAdults, maxChildren)),
        DEFAULT_BASE_PRICE,
        true,
        now,
        now,
        0L);
  }

  // HT17: field null được giữ nguyên; sức chứa kiểm trên giá trị sau khi gộp với dữ liệu hiện tại.
  public RoomType update(
      String name,
      String description,
      Short maxAdults,
      Short maxChildren,
      Short maxOccupancy,
      Long basePrice,
      Boolean active,
      Instant now) {
    if (now == null) {
      throw HotelException.invalid("Thời điểm cập nhật loại phòng là bắt buộc.");
    }

    String newName = this.name;
    if (name != null) {
      newName = name.trim();
      if (newName.isEmpty()) {
        throw HotelException.invalidRequest("Tên loại phòng không được để trống.");
      }
    }

    String newDescription = description == null ? this.description : description.trim();

    short newAdults = maxAdults != null ? maxAdults : this.maxAdults;
    short newChildren = maxChildren != null ? maxChildren : this.maxChildren;
    short newOccupancy = maxOccupancy != null ? maxOccupancy : this.maxOccupancy;

    if (newAdults < 1) {
      throw HotelException.invalidOccupancy("Số người lớn tối đa phải ít nhất là 1.");
    }
    if (newChildren < 0) {
      throw HotelException.invalidOccupancy("Số trẻ em tối đa không được âm.");
    }
    if (newOccupancy < 1) {
      throw HotelException.invalidOccupancy("Sức chứa tối đa phải ít nhất là 1.");
    }
    if (newAdults > newOccupancy) {
      throw HotelException.invalidOccupancy(
          "Số người lớn ("
              + newAdults
              + ") không được vượt quá sức chứa tối đa ("
              + newOccupancy
              + ").");
    }
    if (newChildren > newOccupancy) {
      throw HotelException.invalidOccupancy(
          "Số trẻ em tối đa không được vượt quá sức chứa tối đa.");
    }

    long newBasePrice = this.basePrice;
    if (basePrice != null) {
      if (basePrice < 0) {
        throw HotelException.invalidPrice("Giá cơ bản phải lớn hơn hoặc bằng 0.");
      }
      newBasePrice = basePrice;
    }

    boolean newActive = active != null ? active : this.active;

    return new RoomType(
        id,
        hotelId,
        newName,
        newDescription,
        newAdults,
        newChildren,
        newOccupancy,
        newBasePrice,
        newActive,
        createdAt,
        now,
        version);
  }
}
