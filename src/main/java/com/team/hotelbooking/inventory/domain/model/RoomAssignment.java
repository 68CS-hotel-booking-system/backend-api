package com.team.hotelbooking.inventory.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomAssignment(
    UUID id,
    UUID hotelId,
    UUID bookingId,
    UUID bookingItemId,
    UUID roomTypeId,
    UUID roomId,
    String roomNumberSnapshot,
    LocalDate startDate,
    LocalDate endDate,
    Status status,
    UUID assignedByUserId,
    Instant assignedAt,
    UUID checkedInByUserId,
    Instant checkedInAt,
    UUID checkedOutByUserId,
    Instant checkedOutAt,
    UUID releasedByUserId,
    Instant releasedAt,
    String releaseReason) {

  public enum Status {
    ASSIGNED,
    CHECKED_IN,
    CHECKED_OUT,
    RELEASED
  }
}
