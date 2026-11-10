package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GetRoomCalendarQuery(
    UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {}
