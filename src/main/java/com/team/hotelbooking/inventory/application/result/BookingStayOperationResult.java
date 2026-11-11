package com.team.hotelbooking.inventory.application.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingStayOperationResult(
    UUID hotelId,
    UUID bookingId,
    String stayStatus,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    Instant lateArrivalExpectedAt,
    String lateArrivalNote,
    int releasedRoomNights,
    Instant processedAt,
    List<RoomAssignmentResult> roomAssignments) {}
