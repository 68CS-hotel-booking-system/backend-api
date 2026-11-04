package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.time.Instant;
import java.util.UUID;

public record ListManagementRoomTypesResult(
    UUID id,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static ListManagementRoomTypesResult from(RoomType roomType) {
    return new ListManagementRoomTypesResult(
        roomType.id(),
        roomType.name(),
        roomType.description(),
        roomType.maxAdults(),
        roomType.maxChildren(),
        roomType.active(),
        roomType.createdAt(),
        roomType.updatedAt(),
        roomType.version());
  }
}
