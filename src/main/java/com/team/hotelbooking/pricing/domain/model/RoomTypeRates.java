package com.team.hotelbooking.pricing.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomTypeRates(
    UUID hotelId,
    UUID roomTypeId,
    String currency,
    LocalDate startDate,
    LocalDate endDate,
    List<DailyRate> rates) {}
