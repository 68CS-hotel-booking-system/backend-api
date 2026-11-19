package com.team.hotelbooking.inventory.application.command;

import java.util.List;
import java.util.UUID;

public record AssignRoomsCommand(
    UUID actorId,
    UUID hotelId,
    UUID bookingId,
    List<RoomAssignmentItem> assignments,
    String reason) {

  public record RoomAssignmentItem(UUID bookingItemId, List<UUID> roomIds) {}
}
