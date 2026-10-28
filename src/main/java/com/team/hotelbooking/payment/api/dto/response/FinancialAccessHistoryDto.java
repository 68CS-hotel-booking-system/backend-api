package com.team.hotelbooking.payment.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FinancialAccessHistoryDto(
    UUID id,
    long version,
    boolean isGranted,
    String accessScope,
    String reason,
    UUID changedByUserId,
    Instant changedAt) {}
