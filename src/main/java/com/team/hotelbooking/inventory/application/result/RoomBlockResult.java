package com.team.hotelbooking.inventory.application.result;

import com.team.hotelbooking.inventory.domain.model.RoomBlock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomBlockResult(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    UUID roomId,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    String status,
    UUID createdByUserId,
    Instant createdAt,
    UUID updatedByUserId,
    Instant updatedAt,
    UUID releasedByUserId,
    Instant releasedAt,
    String releaseReason) {

  public static RoomBlockResult from(RoomBlock domain) {
    return new RoomBlockResult(
        domain.id(),
        domain.hotelId(),
        domain.roomTypeId(),
        domain.roomId(),
        domain.startDate(),
        domain.endDate(),
        domain.reason(),
        domain.status().name(),
        domain.createdByUserId(),
        domain.createdAt(),
        domain.updatedByUserId(),
        domain.updatedAt(),
        domain.releasedByUserId(),
        domain.releasedAt(),
        domain.releaseReason());
  }
}
