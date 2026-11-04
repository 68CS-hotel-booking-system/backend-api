package com.team.hotelbooking.payment.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FinancialAccessDetail(
    UUID id,
    UUID hotelId,
    UUID userId,
    String userName,
    String userEmail,
    boolean granted,
    String accessScope,
    String reason,
    UUID grantedByUserId,
    Instant grantedAt,
    long version,
    List<FinancialAccessHistory> history) {}
