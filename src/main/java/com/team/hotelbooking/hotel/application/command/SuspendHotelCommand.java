package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// ADMIN/Owner/HOTEL_MANAGER tạm ngừng bán (ACTIVE → SUSPENDED); lý do tùy chọn.
public record SuspendHotelCommand(UUID actorUserId, boolean admin, UUID hotelId, String reason) {}
