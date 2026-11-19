package com.team.hotelbooking.inventory.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomBlock(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    UUID roomId,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    Status status,
    UUID createdByUserId,
    Instant createdAt,
    UUID updatedByUserId,
    Instant updatedAt,
    UUID releasedByUserId,
    Instant releasedAt,
    String releaseReason) {

  public enum Status {
    ACTIVE,
    RELEASED
  }
}
