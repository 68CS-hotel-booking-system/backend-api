package com.team.hotelbooking.hotel.application.command;

import java.util.List;
import java.util.UUID;

// actorUserId và admin lấy từ principal đã xác thực, giống UpdateHotelCommand.
public record ReplaceRoomTypeAmenitiesCommand(
    UUID actorUserId, boolean admin, UUID hotelId, UUID roomTypeId, List<UUID> amenityIds) {}
