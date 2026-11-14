package com.team.hotelbooking.pricing.domain.model;

import com.team.hotelbooking.pricing.domain.service.NightlyPriceResolver;
import java.time.Instant;
import java.time.LocalDate;

public record DailyRate(
    LocalDate date,
    Long price,
    boolean hasRate,
    Instant updatedAt,
    Long effectivePrice,
    PriceSource source) {

  public static DailyRate resolve(
      LocalDate date, long basePrice, Long customRate, Instant updatedAt) {
    var effective = NightlyPriceResolver.resolveNight(basePrice, customRate);
    boolean hasRate = customRate != null && customRate > 0;
    return new DailyRate(
        date,
        hasRate ? customRate : null,
        hasRate,
        hasRate ? updatedAt : null,
        effective.amount(),
        effective.source());
  }
}
