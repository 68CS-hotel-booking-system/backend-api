package com.team.hotelbooking.inventory.application.result;

import java.time.Instant;
import java.util.UUID;

public record RoomReadinessResult(
    UUID hotelId,
    UUID roomId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    String readiness,
    Instant updatedAt) {}
