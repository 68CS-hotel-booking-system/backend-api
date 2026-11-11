package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GetRoomCalendarQuery(
    UUID actorId, UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {

  public GetRoomCalendarQuery(
      UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    this(null, hotelId, roomTypeId, startDate, endDate);
  }
}
