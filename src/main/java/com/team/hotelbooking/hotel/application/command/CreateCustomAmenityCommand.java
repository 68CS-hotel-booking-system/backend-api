package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record CreateCustomAmenityCommand(
    UUID actorUserId, boolean admin, UUID hotelId, UUID roomId, String name, String description) {}
