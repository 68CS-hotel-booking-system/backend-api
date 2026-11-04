package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record GetManagementHotelQuery(UUID actorId, UUID hotelId) {}
