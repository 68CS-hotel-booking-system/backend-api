package com.team.hotelbooking.hotel.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HotelPublishReadinessResult(
    UUID hotelId,
    boolean ready,
    long operationalRoomCount,
    Instant checkedAt,
    List<Requirement> unmetRequirements) {
  public record Requirement(String field, String message) {}
}
