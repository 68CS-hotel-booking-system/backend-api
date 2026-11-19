package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record UpdateCustomAmenityCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
    UUID roomId,
    UUID amenityId,
    String name,
    String description,
    Boolean attached,
    Long expectedVersion) {}
