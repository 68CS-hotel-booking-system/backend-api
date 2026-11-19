package com.team.hotelbooking.hotel.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

/** roomTypeId null nghĩa là không lọc theo loại phòng. */
public record ListManagementRoomsQuery(
    UUID actorId, UUID hotelId, UUID roomTypeId, PageQuery pagination) {}
