package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record CreateRoomTypeCommand(
    UUID actorId,
    UUID hotelId,
    String name,
    String description,
    Short maxAdults,
    Short maxChildren) {}
