package com.team.hotelbooking.hotel.application.result;

import com.team.hotelbooking.hotel.domain.model.Room;
import java.time.Instant;
import java.util.UUID;

public record RoomResult(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    Short floor,
    String status,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  // Hợp đồng HT21: READY → "ready", DIRTY → "cleaning"; tầng không phải số thì trả null.
  public static RoomResult from(Room room, Instant now) {
    return new RoomResult(
        room.id(),
        room.hotelId(),
        room.roomTypeId(),
        room.roomNumber(),
        floorNumber(room.floor()),
        room.readiness() == Room.Readiness.READY ? "ready" : "cleaning",
        room.activeAt(now),
        room.createdAt(),
        room.updatedAt());
  }

  private static Short floorNumber(String floor) {
    if (floor == null) {
      return null;
    }
    try {
      return Short.valueOf(floor);
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
