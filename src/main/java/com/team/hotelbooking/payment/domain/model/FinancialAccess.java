package com.team.hotelbooking.payment.domain.model;

import java.time.Instant;
import java.util.UUID;

public record FinancialAccess(
    UUID id,
    UUID hotelId,
    UUID userId,
    boolean granted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version) {}
