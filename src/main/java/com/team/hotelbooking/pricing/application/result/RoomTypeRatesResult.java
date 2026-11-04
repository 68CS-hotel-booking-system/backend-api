package com.team.hotelbooking.pricing.application.result;

import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomTypeRatesResult(
    UUID hotelId,
    UUID roomTypeId,
    String currency,
    LocalDate startDate,
    LocalDate endDate,
    List<DailyRateResult> rates) {

  public static RoomTypeRatesResult from(RoomTypeRates domain) {
    List<DailyRateResult> rates = domain.rates().stream().map(DailyRateResult::from).toList();

    return new RoomTypeRatesResult(
        domain.hotelId(),
        domain.roomTypeId(),
        domain.currency(),
        domain.startDate(),
        domain.endDate(),
        rates);
  }
}
