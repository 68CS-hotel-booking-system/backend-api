package com.team.hotelbooking.pricing.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoomRate(
    UUID roomTypeId,
    LocalDate rateDate,
    Long amount,
    String currency,
    UUID updatedByUserId,
    Instant createdAt,
    Instant updatedAt) {}
