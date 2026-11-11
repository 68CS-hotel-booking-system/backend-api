package com.team.hotelbooking.inventory.application.command;

import java.util.UUID;

public record MarkNoShowCommand(UUID actorId, UUID hotelId, UUID bookingId, String reason) {}
