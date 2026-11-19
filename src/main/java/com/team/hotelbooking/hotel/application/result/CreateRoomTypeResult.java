package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomTypeResult(
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

  public static CreateRoomTypeResult from(RoomType roomType) {
    return new CreateRoomTypeResult(
        roomType.id(),
        roomType.hotelId(),
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
