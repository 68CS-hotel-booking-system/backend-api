package com.team.hotelbooking.inventory.api.dto.response;

import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingStayOperationResponse(
    UUID hotelId,
    UUID bookingId,
    String stayStatus,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    Instant lateArrivalExpectedAt,
    String lateArrivalNote,
    int releasedRoomNights,
    Instant processedAt,
    List<RoomAssignmentResponse> roomAssignments) {

  public static BookingStayOperationResponse from(BookingStayOperationResult result) {
    return new BookingStayOperationResponse(
        result.hotelId(),
        result.bookingId(),
        result.stayStatus(),
        result.checkInDate(),
        result.checkOutDate(),
        result.lateArrivalExpectedAt(),
        result.lateArrivalNote(),
        result.releasedRoomNights(),
        result.processedAt(),
        result.roomAssignments().stream().map(RoomAssignmentResponse::from).toList());
  }
}
