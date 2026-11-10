package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// actorUserId và admin lấy từ principal; quyền theo hotel kiểm trong use case.
public record UpdateRoomTypeCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
    UUID roomTypeId,
    String name,
    String description,
    Short maxAdults,
    Short maxChildren,
    Short maxOccupancy,
    Long basePrice,
    Boolean active) {}
