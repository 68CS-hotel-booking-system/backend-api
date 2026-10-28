package com.team.hotelbooking.payment.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FinancialAccessDto(
    UUID id,
    UUID hotelId,
    UUID userId,
    boolean isGranted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version) {}
