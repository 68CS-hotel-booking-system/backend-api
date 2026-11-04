package com.team.hotelbooking.pricing.application.result;

import com.team.hotelbooking.pricing.domain.model.DailyRate;
import java.time.Instant;
import java.time.LocalDate;

public record DailyRateResult(LocalDate date, Long price, boolean hasRate, Instant updatedAt) {

  public static DailyRateResult from(DailyRate domain) {
    return new DailyRateResult(domain.date(), domain.price(), domain.hasRate(), domain.updatedAt());
  }
}
