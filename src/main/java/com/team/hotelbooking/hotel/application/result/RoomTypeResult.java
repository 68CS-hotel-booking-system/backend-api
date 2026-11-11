package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.RoomType;
import java.time.Instant;
import java.util.UUID;

public record RoomTypeResult(
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
    Instant updatedAt) {

  public static RoomTypeResult from(RoomType roomType) {
    return new RoomTypeResult(
        roomType.id(),
        roomType.hotelId(),
        roomType.name(),
        roomType.description(),
        roomType.maxAdults(),
        roomType.maxChildren(),
        roomType.maxOccupancy(),
        roomType.basePrice(),
        roomType.active(),
        roomType.createdAt(),
        roomType.updatedAt());
  }
}
