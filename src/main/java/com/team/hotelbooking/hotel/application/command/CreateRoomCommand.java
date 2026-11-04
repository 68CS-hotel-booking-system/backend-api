package com.team.hotelbooking.hotel.application.command;

import java.time.Instant;
import java.util.UUID;

public record CreateRoomCommand(
    UUID actorId,
    UUID hotelId,
    UUID roomTypeId,
    String roomNumber,
    String floor,
    Instant operationalFrom,
    Instant operationalUntil) {}
