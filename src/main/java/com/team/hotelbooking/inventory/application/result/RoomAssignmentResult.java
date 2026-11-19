package com.team.hotelbooking.inventory.application.result;

import com.team.hotelbooking.inventory.domain.model.RoomAssignment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomAssignmentResult(
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

  public static RoomAssignmentResult from(RoomAssignment domain) {
    return new RoomAssignmentResult(
        domain.id(),
        domain.hotelId(),
        domain.bookingId(),
        domain.bookingItemId(),
        domain.roomTypeId(),
        domain.roomId(),
        domain.roomNumberSnapshot(),
        domain.startDate(),
        domain.endDate(),
        domain.status().name(),
        domain.assignedByUserId(),
        domain.assignedAt(),
        domain.checkedInByUserId(),
        domain.checkedInAt(),
        domain.checkedOutByUserId(),
        domain.checkedOutAt(),
        domain.releasedByUserId(),
        domain.releasedAt(),
        domain.releaseReason());
  }
}
