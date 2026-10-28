package com.team.hotelbooking.pricing.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record DailyRateDto(LocalDate date, Long price, boolean hasRate, Instant updatedAt) {}
