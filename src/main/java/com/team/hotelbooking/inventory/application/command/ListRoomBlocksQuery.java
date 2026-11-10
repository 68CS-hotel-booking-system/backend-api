package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record ListRoomBlocksQuery(
    UUID actorId,
    UUID hotelId,
    UUID roomTypeId,
    UUID roomId,
    LocalDate startDate,
    LocalDate endDate) {

  public ListRoomBlocksQuery(
      UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate) {
    this(null, hotelId, roomTypeId, roomId, startDate, endDate);
  }
}
