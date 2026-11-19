package com.team.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record CheckAvailabilityQuery(
    UUID hotelId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    Integer adults,
    Integer children,
    Integer quantity) {}
