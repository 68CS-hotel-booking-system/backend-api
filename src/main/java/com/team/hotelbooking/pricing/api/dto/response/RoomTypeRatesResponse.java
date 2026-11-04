package com.team.hotelbooking.pricing.api.dto.response;

import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomTypeRatesResponse(
    UUID hotelId,
    UUID roomTypeId,
    String currency,
    LocalDate startDate,
    LocalDate endDate,
    List<DailyRateDto> rates) {

  public static RoomTypeRatesResponse from(RoomTypeRatesResult result) {
    List<DailyRateDto> dtos = result.rates().stream().map(DailyRateDto::from).toList();

    return new RoomTypeRatesResponse(
        result.hotelId(),
        result.roomTypeId(),
        result.currency(),
        result.startDate(),
        result.endDate(),
        dtos);
  }
}
