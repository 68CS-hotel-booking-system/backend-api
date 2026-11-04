package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.CreateRoomTypeResult;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomTypeResponse(
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

  public static CreateRoomTypeResponse from(CreateRoomTypeResult result) {
    return new CreateRoomTypeResponse(
        result.id(),
        result.hotelId(),
        result.name(),
        result.description(),
        result.maxAdults(),
        result.maxChildren(),
        result.active(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }
}
