package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record GetRoomTypeAmenitiesQuery(
    UUID actorUserId, boolean admin, UUID hotelId, UUID roomTypeId) {}
