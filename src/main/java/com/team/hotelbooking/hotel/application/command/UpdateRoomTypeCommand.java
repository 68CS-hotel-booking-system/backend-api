package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

public record UpdateRoomTypeCommand(
    UUID hotelId,
    UUID roomTypeId,
    String name,
    String description,
    Short maxAdults,
    Short maxChildren,
    Short maxOccupancy,
    Long basePrice,
    Boolean active) {}
