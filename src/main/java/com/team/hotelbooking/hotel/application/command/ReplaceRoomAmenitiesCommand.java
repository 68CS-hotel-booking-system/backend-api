package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record ReplaceRoomAmenitiesCommand(
    UUID actorUserId, boolean admin, UUID hotelId, UUID roomId, java.util.List<UUID> amenityIds) {}
