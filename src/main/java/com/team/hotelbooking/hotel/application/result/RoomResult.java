package com.team.hotelbooking.hotel.application.result;

import java.time.Instant;
import java.util.UUID;

public record RoomResult(
    UUID id,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    Short floor,
    String status,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}

