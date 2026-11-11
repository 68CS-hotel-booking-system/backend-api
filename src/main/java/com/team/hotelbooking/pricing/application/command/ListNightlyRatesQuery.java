package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ListNightlyRatesQuery(Set<UUID> roomTypeIds, LocalDate checkIn, LocalDate checkOut) {

  public ListNightlyRatesQuery {
    roomTypeIds = roomTypeIds == null ? Set.of() : Set.copyOf(roomTypeIds);
  }
}
