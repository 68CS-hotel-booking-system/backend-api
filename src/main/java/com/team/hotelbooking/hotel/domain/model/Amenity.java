package com.team.hotelbooking.hotel.domain.model;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record Amenity(UUID id, String code, String name, Category category) {

  public enum Category {
    HOTEL,
    ROOM
  }

  // Khách sạn chỉ gắn tiện ích HOTEL, loại phòng chỉ gắn tiện ích ROOM; mọi id phải tồn tại.
  public static void requireAssignable(
      Set<UUID> requestedIds, Collection<Amenity> found, Category category) {

    if (requestedIds == null
        || requestedIds.stream().anyMatch(Objects::isNull)
        || category == null) {
      throw new HotelException(
          HotelErrorCode.VALIDATION_FAILED, "Danh sách tiện ích không được chứa giá trị rỗng.");
    }

    Set<UUID> foundIds = found.stream().map(Amenity::id).collect(Collectors.toSet());
    if (!foundIds.containsAll(requestedIds)) {
      throw new HotelException(HotelErrorCode.AMENITY_NOT_FOUND);
    }

    if (found.stream().anyMatch(amenity -> amenity.category() != category)) {
      throw new HotelException(
          HotelErrorCode.AMENITY_CATEGORY_MISMATCH,
          category == Category.HOTEL
              ? "Khách sạn chỉ được gắn tiện ích thuộc nhóm HOTEL."
              : "Loại phòng chỉ được gắn tiện ích thuộc nhóm ROOM.");
    }
  }
}
