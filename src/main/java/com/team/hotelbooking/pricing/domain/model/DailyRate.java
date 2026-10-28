package com.team.hotelbooking.pricing.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record DailyRate(LocalDate date, Long price, boolean hasRate, Instant updatedAt) {}
