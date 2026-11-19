package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record GetManagementAmenitiesQuery(
    UUID actorUserId, boolean admin, UUID hotelId, UUID roomId) {}
