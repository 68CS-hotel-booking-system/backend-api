package com.team.hotelbooking.hotel.application.command;

import java.util.UUID;

// ADMIN/Owner/HOTEL_MANAGER mở bán lại (SUSPENDED → ACTIVE); lý do tùy chọn.
public record ResumeHotelCommand(UUID actorUserId, boolean admin, UUID hotelId, String reason) {}
