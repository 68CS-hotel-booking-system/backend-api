package com.team.hotelbooking.inventory.application.command;

import java.util.UUID;

public record CheckOutBookingCommand(UUID actorId, UUID hotelId, UUID bookingId) {}
