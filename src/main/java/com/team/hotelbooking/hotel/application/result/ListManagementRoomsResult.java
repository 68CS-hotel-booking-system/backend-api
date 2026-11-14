package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record ListManagementRoomsResult(
    UUID id,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    String readiness,
    String operationalStatus,
    Instant readinessUpdatedAt,
    Instant operationalFrom,
    Instant operationalUntil,
    Instant createdAt,
    Instant updatedAt,
    long version) {

  public static ListManagementRoomsResult from(Room room, Instant at) {
    return new ListManagementRoomsResult(
        room.id(),
        room.roomTypeId(),
        room.roomNumber(),
        room.floor(),
        room.readiness().name(),
        room.operationalStatusAt(at).name(),
        room.readinessUpdatedAt(),
        room.operationalFrom(),
        room.operationalUntil(),
        room.createdAt(),
        room.updatedAt(),
        room.version());
  }
}
