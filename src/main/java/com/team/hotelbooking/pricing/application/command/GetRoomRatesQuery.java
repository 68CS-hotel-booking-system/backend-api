package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GetRoomRatesQuery(
    UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate) {}
