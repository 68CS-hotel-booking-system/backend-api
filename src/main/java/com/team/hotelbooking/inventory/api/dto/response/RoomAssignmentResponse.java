package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.RoomAssignmentResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomAssignmentResponse(
    UUID id,
    UUID hotelId,
    UUID bookingId,
    UUID bookingItemId,
    UUID roomTypeId,
    UUID roomId,
    String roomNumberSnapshot,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    UUID assignedByUserId,
    Instant assignedAt,
    UUID checkedInByUserId,
    Instant checkedInAt,
    UUID checkedOutByUserId,
    Instant checkedOutAt,
    UUID releasedByUserId,
    Instant releasedAt,
    String releaseReason) {

  public static RoomAssignmentResponse from(RoomAssignmentResult result) {
    return new RoomAssignmentResponse(
        result.id(),
        result.hotelId(),
        result.bookingId(),
        result.bookingItemId(),
        result.roomTypeId(),
        result.roomId(),
        result.roomNumberSnapshot(),
        result.startDate(),
        result.endDate(),
        result.status(),
        result.assignedByUserId(),
        result.assignedAt(),
        result.checkedInByUserId(),
        result.checkedInAt(),
        result.checkedOutByUserId(),
        result.checkedOutAt(),
        result.releasedByUserId(),
        result.releasedAt(),
        result.releaseReason());
  }
}
