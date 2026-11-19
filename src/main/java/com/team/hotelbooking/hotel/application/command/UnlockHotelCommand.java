package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// ADMIN gỡ khóa bán; lý do bắt buộc.
public record UnlockHotelCommand(UUID actorUserId, UUID hotelId, String reason) {}
