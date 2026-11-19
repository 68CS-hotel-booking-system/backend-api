package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record DetachCustomAmenityCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
    UUID roomId,
    UUID amenityId,
    Long expectedVersion) {}
