package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreateRoomBlockCommand(
    UUID actorId,
    UUID hotelId,
    UUID roomId,
    LocalDate startDate,
    LocalDate endDate,
    String reason) {}
