package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GetRoomRatesQuery(
    UUID actorId, UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {

  public GetRoomRatesQuery(UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {
    this(null, hotelId, roomTypeId, startDate, endDate);
  }
}
