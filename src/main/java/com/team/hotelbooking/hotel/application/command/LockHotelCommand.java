package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// ADMIN khóa bán; lý do bắt buộc.
public record LockHotelCommand(UUID actorUserId, UUID hotelId, String reason) {}
