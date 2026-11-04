package com.team.hotelbooking.pricing.api.dto.response;

import com.team.hotelbooking.pricing.application.result.DailyRateResult;
import java.time.Instant;
import java.time.LocalDate;

public record DailyRateDto(LocalDate date, Long price, boolean hasRate, Instant updatedAt) {

  public static DailyRateDto from(DailyRateResult result) {
    return new DailyRateDto(result.date(), result.price(), result.hasRate(), result.updatedAt());
  }
}
