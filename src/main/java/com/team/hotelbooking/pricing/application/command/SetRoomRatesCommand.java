package com.team.hotelbooking.pricing.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record SetRoomRatesCommand(
    UUID hotelId,
    UUID roomTypeId,
    LocalDate startDate,
    LocalDate endDateExclusive,
    Long amount,
    String currency) {}
