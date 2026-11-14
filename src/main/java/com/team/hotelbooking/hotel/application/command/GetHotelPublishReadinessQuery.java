package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record GetHotelPublishReadinessQuery(UUID actorId, boolean admin, UUID hotelId) {}
