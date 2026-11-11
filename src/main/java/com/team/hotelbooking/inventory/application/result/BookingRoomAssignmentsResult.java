package com.team.hotelbooking.inventory.application.result;

import java.util.List;
import java.util.UUID;

public record BookingRoomAssignmentsResult(
    UUID hotelId,
    UUID bookingId,
    List<RoomAssignmentResult> activeAssignments,
    List<RoomAssignmentResult> history) {}
