package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.RoomCalendarResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomCalendarResponse(
    UUID hotelId,
    LocalDate startDate,
    LocalDate endDate,
    List<RoomTypeCalendarResponse> roomTypes) {

  public record RoomTypeCalendarResponse(
      UUID roomTypeId,
      String roomTypeName,
      List<PhysicalRoomCalendarResponse> rooms,
      List<DailyInventorySummaryResponse> dailySummaries) {}

  public record PhysicalRoomCalendarResponse(
      UUID roomId,
      String roomNumber,
      String floor,
      String readiness,
      Instant operationalFrom,
      Instant operationalUntil,
      List<RoomBlockResponse> activeBlocks,
      List<RoomAssignmentResponse> activeAssignments) {}

  public record DailyInventorySummaryResponse(
      LocalDate date,
      int operationalRooms,
      int blockedRooms,
      int reservedRooms,
      int availableRooms) {}

  public static RoomCalendarResponse from(RoomCalendarResult result) {
    List<RoomTypeCalendarResponse> roomTypes =
        result.roomTypes().stream()
            .map(
                rt ->
                    new RoomTypeCalendarResponse(
                        rt.roomTypeId(),
                        rt.roomTypeName(),
                        rt.rooms().stream()
                            .map(
                                r ->
                                    new PhysicalRoomCalendarResponse(
                                        r.roomId(),
                                        r.roomNumber(),
                                        r.floor(),
                                        r.readiness(),
                                        r.operationalFrom(),
                                        r.operationalUntil(),
                                        r.activeBlocks().stream()
                                            .map(RoomBlockResponse::from)
                                            .toList(),
                                        r.activeAssignments() == null
                                            ? List.of()
                                            : r.activeAssignments().stream()
                                                .map(RoomAssignmentResponse::from)
                                                .toList()))
                            .toList(),
                        rt.dailySummaries().stream()
                            .map(
                                d ->
                                    new DailyInventorySummaryResponse(
                                        d.date(),
                                        d.operationalRooms(),
                                        d.blockedRooms(),
                                        d.reservedRooms(),
                                        d.availableRooms()))
                            .toList()))
            .toList();

    return new RoomCalendarResponse(
        result.hotelId(), result.startDate(), result.endDate(), roomTypes);
  }
}
