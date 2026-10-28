package com.team.hotelbooking.pricing.api.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomTypeRatesResponse(
    UUID hotelId,
    UUID roomTypeId,
    String currency,
    LocalDate startDate,
    LocalDate endDate,
    List<DailyRateDto> rates) {}
