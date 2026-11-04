package com.team.hotelbooking.hotel.api.dto.response;

import com.team.hotelbooking.hotel.application.result.CreateRoomResult;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomResponse(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    String readiness,
    Instant readinessUpdatedAt,
    UUID readinessUpdatedBy,
    Instant operationalFrom,
    Instant operationalUntil,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static CreateRoomResponse from(CreateRoomResult result) {
    return new CreateRoomResponse(
        result.id(),
        result.hotelId(),
        result.roomTypeId(),
        result.roomNumber(),
        result.floor(),
        result.readiness(),
        result.readinessUpdatedAt(),
        result.readinessUpdatedBy(),
        result.operationalFrom(),
        result.operationalUntil(),
        result.createdAt(),
        result.updatedAt(),
        result.version());
  }
}
