package com.team.hotelbooking.inventory.application.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomCalendarResult(
    UUID hotelId, LocalDate startDate, LocalDate endDate, List<RoomTypeCalendarResult> roomTypes) {

  public record RoomTypeCalendarResult(
      UUID roomTypeId,
      String roomTypeName,
      List<PhysicalRoomCalendarResult> rooms,
      List<DailyInventorySummaryResult> dailySummaries) {}

  public record PhysicalRoomCalendarResult(
      UUID roomId,
      String roomNumber,
      String floor,
      String readiness,
      Instant operationalFrom,
      Instant operationalUntil,
      List<RoomBlockResult> activeBlocks,
      List<RoomAssignmentResult> activeAssignments) {

    public PhysicalRoomCalendarResult(
        UUID roomId,
        String roomNumber,
        String floor,
        String readiness,
        Instant operationalFrom,
        Instant operationalUntil,
        List<RoomBlockResult> activeBlocks) {
      this(
          roomId,
          roomNumber,
          floor,
          readiness,
          operationalFrom,
          operationalUntil,
          activeBlocks,
          List.of());
    }
  }

  public record DailyInventorySummaryResult(
      LocalDate date,
      int operationalRooms,
      int blockedRooms,
      int reservedRooms,
      int availableRooms) {}
}
