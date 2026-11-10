package com.team.hotelbooking.media.domain.model;

import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import java.time.Instant;
import java.util.UUID;

public record HotelImageAlbum(
    UUID id,
    UUID hotelId,
    String name,
    String description,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt) {

  public HotelImageAlbum {
    if (id == null || hotelId == null || createdAt == null || updatedAt == null || sortOrder < 0) {
      throw invalid("Thông tin album không hợp lệ.");
    }

    name = normalizeName(name);
    description = normalizeDescription(description);
  }

  public HotelImageAlbum update(String newName, String newDescription, Instant now) {

    return new HotelImageAlbum(
        id,
        hotelId,
        newName == null ? name : newName,
        newDescription == null ? description : newDescription,
        sortOrder,
        createdAt,
        now);
  }

  public HotelImageAlbum reorder(int newOrder, Instant now) {
    return new HotelImageAlbum(id, hotelId, name, description, newOrder, createdAt, now);
  }

  private static String normalizeName(String value) {
    String normalized = value == null ? "" : value.trim();

    if (normalized.isBlank() || normalized.length() > 100) {
      throw invalid("Tên album phải có từ 1 đến 100 ký tự.");
    }

    return normalized;
  }

  private static String normalizeDescription(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();

    if (normalized.length() > 2000) {
      throw invalid("Mô tả album không được vượt quá 2000 ký tự.");
    }

    return normalized.isEmpty() ? null : normalized;
  }

  private static MediaException invalid(String message) {
    return new MediaException(MediaErrorCode.VALIDATION_FAILED, message);
  }
}
