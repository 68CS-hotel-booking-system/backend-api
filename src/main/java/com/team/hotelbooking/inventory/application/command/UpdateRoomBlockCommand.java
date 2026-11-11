package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateRoomBlockCommand(
    UUID actorId,
    UUID hotelId,
    UUID blockId,
    LocalDate startDate,
    LocalDate endDate,
    String reason) {}
