package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record CreateRoomResult(
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

  public static CreateRoomResult from(Room room) {
    return new CreateRoomResult(
        room.id(),
        room.hotelId(),
        room.roomTypeId(),
        room.roomNumber(),
        room.floor(),
        room.readiness().name(),
        room.readinessUpdatedAt(),
        room.readinessUpdatedBy(),
        room.operationalFrom(),
        room.operationalUntil(),
        room.createdAt(),
        room.updatedAt(),
        room.version());
  }
}
