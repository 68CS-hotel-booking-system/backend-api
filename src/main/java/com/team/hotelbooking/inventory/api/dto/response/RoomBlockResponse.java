package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomBlockResponse(
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

  public static RoomBlockResponse from(RoomBlockResult result) {
    return new RoomBlockResponse(
        result.id(),
        result.hotelId(),
        result.roomTypeId(),
        result.roomId(),
        result.startDate(),
        result.endDate(),
        result.reason(),
        result.status(),
        result.createdByUserId(),
        result.createdAt(),
        result.updatedByUserId(),
        result.updatedAt(),
        result.releasedByUserId(),
        result.releasedAt(),
        result.releaseReason());
  }

  public static List<RoomBlockResponse> from(List<RoomBlockResult> results) {
    return results.stream().map(RoomBlockResponse::from).toList();
  }
}
