package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record CustomAmenity(
    UUID id,
    UUID hotelId,
    UUID roomId,
    String name,
    String normalizedName,
    String description,
    boolean attached,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static CustomAmenity create(
      UUID id, UUID hotelId, UUID roomId, String name, String description, Instant now) {
    if (id == null || hotelId == null || now == null)
      throw HotelException.invalid("Định danh và thời điểm tạo là bắt buộc.");
    String cleaned = cleanName(name);
    return new CustomAmenity(
        id,
        hotelId,
        roomId,
        cleaned,
        normalizeName(cleaned),
        cleanDescription(description),
        true,
        now,
        now,
        0);
  }

  public CustomAmenity update(String name, String description, Boolean attached, Instant now) {
    if (now == null) throw HotelException.invalid("Thời điểm cập nhật là bắt buộc.");
    if (name == null && description == null && attached == null)
      throw HotelException.invalidRequest("Cần cung cấp tên, mô tả hoặc trạng thái gắn tiện ích.");
    String cleaned = name == null ? this.name : cleanName(name);
    return new CustomAmenity(
        id,
        hotelId,
        roomId,
        cleaned,
        normalizeName(cleaned),
        description == null ? this.description : cleanDescription(description),
        attached == null ? this.attached : attached,
        createdAt,
        now,
        version);
  }

  private static String cleanName(String value) {
    String name =
        value == null
            ? ""
            : Normalizer.normalize(value, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
    if (name.isBlank() || name.length() > 100)
      throw HotelException.invalid("Tên tiện ích phải có từ 1 đến 100 ký tự.");
    return name;
  }

  public static String normalizeName(String value) {
    return cleanName(value).toLowerCase(Locale.ROOT);
  }

  private static String cleanDescription(String value) {
    if (value == null) return null;
    String description = value.strip();
    if (description.length() > 2000)
      throw HotelException.invalid("Mô tả tiện ích tối đa 2000 ký tự.");
    return description.isEmpty() ? null : description;
  }
}
