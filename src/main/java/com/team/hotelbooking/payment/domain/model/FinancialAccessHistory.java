package com.team.hotelbooking.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public record FinancialAccessHistory(
    UUID id,
    UUID financialAccessId,
    UUID hotelId,
    UUID userId,
    boolean granted,
    String accessScope,
    String reason,
    long version,
    UUID changedByUserId,
    Instant changedAt) {}
