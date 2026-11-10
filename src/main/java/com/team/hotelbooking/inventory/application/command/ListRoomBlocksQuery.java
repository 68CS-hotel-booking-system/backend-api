package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record ListRoomBlocksQuery(
    UUID hotelId, UUID roomTypeId, UUID roomId, LocalDate startDate, LocalDate endDate) {}
