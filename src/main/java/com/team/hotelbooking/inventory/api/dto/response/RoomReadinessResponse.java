package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import java.time.Instant;
import java.util.UUID;

public record RoomReadinessResponse(
    UUID hotelId,
    UUID roomId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    String readiness,
    Instant updatedAt) {

  public static RoomReadinessResponse from(RoomReadinessResult result) {
    return new RoomReadinessResponse(
        result.hotelId(),
        result.roomId(),
        result.roomTypeId(),
        result.roomNumber(),
        result.floor(),
        result.readiness(),
        result.updatedAt());
  }
}
