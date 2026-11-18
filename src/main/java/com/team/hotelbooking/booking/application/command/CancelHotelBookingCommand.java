package com.team.hotelbooking.booking.application.command;

import java.util.UUID;

public record CancelHotelBookingCommand(
    UUID actorId, UUID hotelId, UUID bookingId, String reason) {}
