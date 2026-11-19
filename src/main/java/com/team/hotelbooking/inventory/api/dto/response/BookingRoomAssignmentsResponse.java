package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.BookingRoomAssignmentsResult;
import java.util.List;
import java.util.UUID;

public record BookingRoomAssignmentsResponse(
    UUID hotelId,
    UUID bookingId,
    List<RoomAssignmentResponse> activeAssignments,
    List<RoomAssignmentResponse> history) {

  public static BookingRoomAssignmentsResponse from(BookingRoomAssignmentsResult result) {
    return new BookingRoomAssignmentsResponse(
        result.hotelId(),
        result.bookingId(),
        result.activeAssignments().stream().map(RoomAssignmentResponse::from).toList(),
        result.history().stream().map(RoomAssignmentResponse::from).toList());
  }
}
