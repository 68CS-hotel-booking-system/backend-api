package com.team.hotelbooking.booking.application.command;

import com.team.hotelbooking.shared.pagination.PageQuery;
import java.util.UUID;

public record ListManagementBookingsQuery(
    UUID actorId, UUID hotelId, String status, PageQuery pagination) {}
