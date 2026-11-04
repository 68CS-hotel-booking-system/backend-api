package com.team.hotelbooking.hotel.application.result;

import java.time.Instant;
import java.util.UUID;

public record RoomTypeResult(
    UUID id,
    UUID hotelId,
    String name,
    String description,
    short maxAdults,
    short maxChildren,
    short maxOccupancy,
    long basePrice,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
