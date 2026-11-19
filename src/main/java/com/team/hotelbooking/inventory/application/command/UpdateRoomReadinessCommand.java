package com.team.hotelbooking.inventory.application.command;

import java.util.UUID;

public record UpdateRoomReadinessCommand(
    UUID actorId, UUID hotelId, UUID roomId, String readiness) {}
