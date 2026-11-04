package com.team.hotelbooking.hotel.application.command;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record CreateHotelCommand(
    UUID actorId,
    UUID ownerId,
    UUID cityId,
    String name,
    String description,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    Short starRating,
    LocalTime checkInTime,
    LocalTime checkOutTime) {}
