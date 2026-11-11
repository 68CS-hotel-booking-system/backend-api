package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// actorUserId và admin lấy từ principal; quyền theo hotel kiểm trong use case.
public record UpdateRoomCommand(
    UUID actorUserId,
    boolean admin,
    UUID hotelId,
    UUID roomId,
    String roomNumber,
    Short floor,
    String status,
    Boolean active) {}
